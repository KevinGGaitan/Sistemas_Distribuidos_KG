/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 18 de Noviembre 2025
#     Materia: Sistemas Distribuidos
**************************************************************/
package sd.library;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/*
 * Actor especializado que procesa RENOVACION y DEVOLUCION.
 * - Suscripción múltiple a topics "RENOVACION" y "DEVOLUCION"
 * - Procesa lógica de renovación y devolución de libros
 * - Gesta límites de renovación (máximo 2 por usuario)
 * - Coordina con GA para actualizar estado
 * - Failover a GA secundario si primario no responde
 * - Cola de pendingUpdates para resincronizar cuando primario vuelva
 *
 * Uso:
 * java ActorSuscriptor <gcPubHost> <gcPubPort> <gaHost1> <gaPort1> <gaHost2> <gaPort2> <gcRespHost> <gcRespPort>
 */

public class ActorSuscriptor {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Configurables
    private static final int GA_RECV_TIMEOUT_MS = 2000;
    private static final int RESYNC_INTERVAL_SEC = 20;
    private static final String PING_ISBN = "__PING__";

    // Concurrency
    private static final Object gaLock = new Object();
    private static final ConcurrentLinkedQueue<String> pendingUpdates = new ConcurrentLinkedQueue<>();
    private static final AtomicReference<ZMQ.Socket> gaPrimRef = new AtomicReference<>();
    private static final AtomicReference<ZMQ.Socket> gaSecRef = new AtomicReference<>();

    public static void main(String[] args) {
        if (args.length < 8) {
            System.out.println("Uso: java ActorSuscriptor <gcPubHost> <gcPubPort> <gaHost1> <gaPort1> <gaHost2> <gaPort2> <gcRespHost> <gcRespPort>");
            System.exit(1);
        }

        // Configuración de endpoints
        String gcPubHost = args[0];
        int gcPubPort = Integer.parseInt(args[1]);
        final String gaHost1 = args[2];
        final int gaPort1 = Integer.parseInt(args[3]);
        final String gaHost2 = args[4];
        final int gaPort2 = Integer.parseInt(args[5]);
        String gcRespHost = args[6];
        int gcRespPort = Integer.parseInt(args[7]);

        try (ZContext ctx = new ZContext()) {
            // SUB socket: suscribir a RENOVACION y DEVOLUCION
            ZMQ.Socket sub = ctx.createSocket(SocketType.SUB);
            sub.connect("tcp://" + gcPubHost + ":" + gcPubPort);
            sub.subscribe("RENOVACION".getBytes());
            sub.subscribe("DEVOLUCION".getBytes());
            System.out.println("ActorSuscriptor suscrito a RENOVACION y DEVOLUCION en " + gcPubHost + ":" + gcPubPort);

            // Inicializar sockets GA primario/secundario
            synchronized (gaLock) {
                ZMQ.Socket gaPrim = ctx.createSocket(SocketType.REQ);
                gaPrim.setReceiveTimeOut(GA_RECV_TIMEOUT_MS);
                gaPrim.connect("tcp://" + gaHost1 + ":" + gaPort1);
                gaPrimRef.set(gaPrim);
                System.out.println("ActorSuscriptor conectado a GA primario " + gaHost1 + ":" + gaPort1);

                ZMQ.Socket gaSec = ctx.createSocket(SocketType.REQ);
                gaSec.setReceiveTimeOut(GA_RECV_TIMEOUT_MS);
                gaSec.connect("tcp://" + gaHost2 + ":" + gaPort2);
                gaSecRef.set(gaSec);
                System.out.println("ActorSuscriptor conectado a GA secundario " + gaHost2 + ":" + gaPort2);
            }

            // REQ socket para respuestas al GC
            ZMQ.Socket gcReq = ctx.createSocket(SocketType.REQ);
            gcReq.connect("tcp://" + gcRespHost + ":" + gcRespPort);

            // Scheduler para resync
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    intentarResync(ctx, gaHost1, gaPort1);
                } catch (Exception e) {
                    System.out.println("Error en resync thread: " + e.getMessage());
                }
            }, RESYNC_INTERVAL_SEC, RESYNC_INTERVAL_SEC, TimeUnit.SECONDS);

            // Loop principal
            while (!Thread.currentThread().isInterrupted()) {
                String topic = sub.recvStr();
                if (topic == null) break;
                String msg = sub.recvStr();
                if (msg == null) break;

                System.out.println("ActorSuscriptor recibió [" + topic + "]: " + msg);
                JsonObject solicitud = JsonParser.parseString(msg).getAsJsonObject();
                String isbn = solicitud.get("isbn").getAsString();
                String usuario = solicitud.get("usuario").getAsString();

                JsonObject getReq = new JsonObject();
                getReq.addProperty("type", "GET_LIBRO");
                getReq.addProperty("isbn", isbn);

                JsonObject respGA = requestConFallback(ctx, getReq, gaHost1, gaPort1);
                if (!respGA.get("estatus").getAsString().equals("OK")) {
                    gcReq.send(respGA.toString());
                    gcReq.recvStr();
                    continue;
                }

                // Extraer datos del libro
                JsonObject libro = respGA.getAsJsonObject("libro");
                JsonArray prestadoA = libro.getAsJsonArray("prestadoA");
                JsonObject renovaciones = libro.getAsJsonObject("renovaciones");
                JsonObject fechaLim = libro.getAsJsonObject("fechaLim");
                int copiasDisponibles = libro.get("copiasDisponibles").getAsInt();

                JsonObject resultado = new JsonObject();

                if (topic.equals("DEVOLUCION")) {
                    boolean tiene = false;
                    for (JsonElement e : prestadoA) {
                        if (e.getAsString().equals(usuario)) {
                            tiene = true;
                            break;
                        }
                    }

                    if (!tiene) {
                        resultado.addProperty("estatus", "ERROR");
                        resultado.addProperty("mensaje", "El usuario no tiene este libro");
                    } else {
                        // Realizar devolución
                        JsonArray nuevaLista = new JsonArray();
                        for (JsonElement e : prestadoA) {
                            if (!e.getAsString().equals(usuario)) nuevaLista.add(e);
                        }
                        libro.add("prestadoA", nuevaLista);
                        renovaciones.remove(usuario);
                        fechaLim.remove(usuario);
                        libro.add("renovaciones", renovaciones);
                        libro.add("fechaLim", fechaLim);
                        libro.addProperty("copiasDisponibles", copiasDisponibles + 1);

                        JsonObject update = new JsonObject();
                        update.addProperty("type", "UPDATE_LIBRO");
                        update.add("libro", libro);

                        JsonObject updateResp = requestConFallback(ctx, update, gaHost1, gaPort1);
                        if (!updateResp.get("estatus").getAsString().equals("OK")) {
                            resultado.addProperty("estatus", "ERROR");
                            resultado.addProperty("mensaje", "No se pudo persistir la devolución en GA");
                        } else {
                            resultado.addProperty("estatus", "OK");
                            resultado.addProperty("mensaje", "Devolución exitosa");
                            resultado.add("libro", libro);
                        }
                    }
                } else if (topic.equals("RENOVACION")) {
                    boolean tiene = false;
                    for (JsonElement e : prestadoA) {
                        if (e.getAsString().equals(usuario)) {
                            tiene = true;
                            break;
                        }
                    }
                    if (!tiene) {
                        resultado.addProperty("estatus", "ERROR");
                        resultado.addProperty("mensaje", "El usuario no tiene este libro para renovar");
                    } else {
                        final int MAX_RENOVACIONES = 2;
                        int count = renovaciones.has(usuario) ? renovaciones.get(usuario).getAsInt() : 0;
                        if (count >= MAX_RENOVACIONES) {
                            resultado.addProperty("estatus", "ERROR");
                            resultado.addProperty("mensaje", "Máximo de renovaciones alcanzado");
                        } else {
                            renovaciones.addProperty(usuario, count + 1);
                            String nuevaFecha = LocalDate.now().plusDays(7).toString();
                            fechaLim.addProperty(usuario, nuevaFecha);
                            libro.add("renovaciones", renovaciones);
                            libro.add("fechaLim", fechaLim);

                            JsonObject update = new JsonObject();
                            update.addProperty("type", "UPDATE_LIBRO");
                            update.add("libro", libro);

                            JsonObject updateResp = requestConFallback(ctx, update, gaHost1, gaPort1);
                            if (!updateResp.get("estatus").getAsString().equals("OK")) {
                                resultado.addProperty("estatus", "ERROR");
                                resultado.addProperty("mensaje", "No se pudo persistir la renovación en GA");
                            } else {
                                resultado.addProperty("estatus", "OK");
                                resultado.addProperty("mensaje", "Renovación exitosa (" + (count + 1) + ")");
                                resultado.add("libro", libro);
                            }
                        }
                    }
                }

                // Enviar resultado al GC
                gcReq.send(resultado.toString());
                gcReq.recvStr(); // ACK
            }
            // cleanup
            scheduler.shutdownNow();
            synchronized (gaLock) {
                ZMQ.Socket p = gaPrimRef.getAndSet(null);
                if (p != null) p.close();
                ZMQ.Socket b = gaSecRef.getAndSet(null);
                if (b != null) b.close();
            }
            gcReq.close();
            sub.close();
        }
    }

    /**
     * requestConFallback: lógica corregida y diferenciada para:
     * - detectar cuando PRIMARIO responde (usar su respuesta, NO fallback),
     * - detectar cuando PRIMARIO falla pero SECUNDARIO responde (usar SECUNDARIO y ENCOLAR update),
     * - detectar cuando AMBOS fallan (devolver error y NO encolar).
     *
     * Retorna: JsonObject con { "estatus": "OK"/"ERROR", ... }
     *
     * Requiere: gaPrimaryRef, gaBackupRef, pendingUpdates, gaLock y ZContext ctx (si recreas sockets).
     */

     private static JsonObject requestConFallback(ZContext ctx, JsonObject peticion, String gaPrimHost, int gaPrimPort) {
        //boolean gaPrimResponded = false;
        //boolean gaSecResponded = false;
        JsonObject gaPrimRespObj = null;
        JsonObject gaSecRespObj = null;

        // 1) Intentar PRIMARIO primero (no hacer fallback si responde)
        synchronized (gaLock) {
            ZMQ.Socket gaPrim = gaPrimRef.get();
            if (gaPrim != null) {
                try {
                    gaPrim.send(peticion.toString());
                    String resp = gaPrim.recvStr();
                    if (resp != null) {
                        //gaPrimResponded = true;
                        gaPrimRespObj = JsonParser.parseString(resp).getAsJsonObject();
                        // return inmediatamente si primario respondió
                        return gaPrimRespObj;
                    } else {
                        // timeout para recreación y probar gaSec
                        System.out.println("[Failover] Timeout primario (sera recreado).");
                        try { gaPrim.close(); } catch (Exception ignored) {}
                        // recrear gaPrim socket para que intentos futuros usen una nueva instancia
                        try {
                            ZMQ.Socket newGaPrim = ctx.createSocket(SocketType.REQ);
                            newGaPrim.setReceiveTimeOut(GA_RECV_TIMEOUT_MS);
                            newGaPrim.connect("tcp://" + gaPrimHost + ":" + gaPrimPort);
                            gaPrimRef.set(newGaPrim);
                        } catch (Exception e) {
                            gaPrimRef.set(null);
                            System.out.println("[Failover] No se pudo recrear socket primario: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[Failover] Excepción comunicando con primario: " + e.getMessage());
                    try { gaPrim.close(); } catch (Exception ignored) {}
                    gaPrimRef.set(null);
                }
            } else {
                // intentar crear socket primario para probar conectividad la próxima vez
                try {
                    ZMQ.Socket newGaPrim = ctx.createSocket(SocketType.REQ);
                    newGaPrim.setReceiveTimeOut(GA_RECV_TIMEOUT_MS);
                    newGaPrim.connect("tcp://" + gaPrimHost + ":" + gaPrimPort);
                    gaPrimRef.set(newGaPrim);
                } catch (Exception e) {
                    // no hay primario
                    gaPrimRef.set(null);
                }
            }

            // 2) Primario no respondió — intentamos SECUNDARIO
            ZMQ.Socket gaSec = gaSecRef.get();
            if (gaSec != null) {
                try {
                    gaSec.send(peticion.toString());
                    String r2 = gaSec.recvStr();
                    if (r2 != null) {
                        //gaSecResponded = true;
                        gaSecRespObj = JsonParser.parseString(r2).getAsJsonObject();

                        // Si la petición fue un UPDATE_LIBRO y solo el secundario respondió,
                        // encolamos la actualización para resync posterior.
                        String type = peticion.has("type") ? peticion.get("type").getAsString() : "";
                        if ("UPDATE_LIBRO".equalsIgnoreCase(type)) {
                            // sólo encolar si PRIMARIO no respondió pero SECUNDARIO sí
                            pendingUpdates.add(peticion.toString());
                            System.out.println("[Failover] UPDATE aplicado en secundario y encolado para resync.");
                        }
                        return gaSecRespObj;
                    } else {
                        System.out.println("[Failover] Secundario no respondió (timeout).");
                    }
                } catch (Exception e) {
                    System.out.println("[Failover] Error comunicando con secundario: " + e.getMessage());
                }
            } else {
                System.out.println("[Failover] No hay socket secundario configurado.");
            }
        } // fin synchronized(gaLock)


        // 3) ni PRIMARIO ni SECUNDARIO respondieron, devolver ERROR y NO encolar
        JsonObject error = new JsonObject();
        error.addProperty("estatus", "ERROR");
        error.addProperty("mensaje", "GA primario y GA secundario no disponibles");
        return error;
    }

    /**
     * intentarResync: intenta reconectar al primario y re-aplicar pendingUpdates (FIFO).
     * - Si primario no responde, no borra la cola.
     * - Si primario responde y aplica una update con estatus OK, remueve esa update de la cola.
     * - Si alguna update falla en el primario (resp con ERROR), la deja en cola y para el resync
     *   (evita perder datos y evita inconsistencias).
     */
    private static void intentarResync(ZContext ctx, String gaPrimHost, int gaPrimPort) {
        synchronized (gaLock) {
            // Comprueba si primario está vivo (ping con un GET_LIBRO especial)
            boolean gaPrimVivo = false;
            ZMQ.Socket gaPrim = gaPrimRef.get();
            if (gaPrim == null) {
                // intentar crear un socket temporal para probar conectividad
                try {
                    ZMQ.Socket tmp = ctx.createSocket(SocketType.REQ);
                    tmp.setReceiveTimeOut(GA_RECV_TIMEOUT_MS);
                    tmp.connect("tcp://" + gaPrimHost + ":" + gaPrimPort);
                    JsonObject ping = new JsonObject();
                    ping.addProperty("type", "GET_LIBRO");
                    ping.addProperty("isbn", PING_ISBN);
                    tmp.send(ping.toString());
                    String pingResp = tmp.recvStr();
                    tmp.close();
                    if (pingResp != null) gaPrimVivo = true;
                } catch (Exception e) {
                    gaPrimVivo = false;
                }
            } else {
                // usar el socket existente para el ping
                try {
                    JsonObject ping = new JsonObject();
                    ping.addProperty("type", "GET_LIBRO");
                    ping.addProperty("isbn", PING_ISBN);
                    gaPrim.send(ping.toString());
                    String pingResp = gaPrim.recvStr();
                    if (pingResp != null) gaPrimVivo = true;
                } catch (Exception e) {
                    // socket primario no sirve actualmente; cerrarlo para reintento futuro
                    try { if (gaPrim != null) gaPrim.close(); } catch (Exception ignored) {}
                    gaPrimRef.set(null);
                    gaPrimVivo = false;
                }
            }

            if (!gaPrimVivo) {
                // primario sigue caído, nada que aplicar
                return;
            }
            
            // Primario está disponible; asegurar que gaPrimRef sea un socket usable
            if (gaPrimRef.get() == null) {
                try {
                    ZMQ.Socket newGaPrim = ctx.createSocket(SocketType.REQ);
                    newGaPrim.setReceiveTimeOut(GA_RECV_TIMEOUT_MS);
                    newGaPrim.connect("tcp://" + gaPrimHost + ":" + gaPrimPort);
                    gaPrimRef.set(newGaPrim);
                } catch (Exception e) {
                    System.out.println("[Resync] No se pudo crear socket primario para resync: " + e.getMessage());
                    return;
                }
            }

            ZMQ.Socket p = gaPrimRef.get();

            // Re-aplicar las updates en orden FIFO; si alguna falla, detener resync para reintentar luego.
            int applied = 0;
            while (!pendingUpdates.isEmpty()) {
                String serialized = pendingUpdates.peek(); // inspeccionar sin remover
                try {
                    p.send(serialized);
                    String r = p.recvStr();
                    if (r != null) {
                        JsonObject respObj = JsonParser.parseString(r).getAsJsonObject();
                        if (respObj.has("estatus") && respObj.get("estatus").getAsString().equalsIgnoreCase("OK")) {
                            // update aplicada en primario -> remover de la cola
                            pendingUpdates.poll();
                            applied++;
                            continue;
                        } else {
                            // primario devolvió ERROR para esta update -> no eliminar, parar resync
                            System.out.println("[Resync] Primario rechazó update: " + r);
                            break;
                        }
                    } else {
                        // primario no respondió al aplicar update -> detener resync
                        System.out.println("[Resync] Primario no respondió al aplicar update.");
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("[Resync] Excepción aplicando update en primario: " + e.getMessage());
                    break;
                }
            }
            System.out.println("[Resync] Finalizado. Aplicadas: " + applied + ". Pendientes: " + pendingUpdates.size());
        }
    }
    
}