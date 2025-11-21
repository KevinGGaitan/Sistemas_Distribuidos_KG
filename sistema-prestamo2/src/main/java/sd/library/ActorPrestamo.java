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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/*
 * Actor especializado que escucha exclusivamente solicitudes de tipo PRESTAMO.
 * - Se suscribe al topic "PRESTAMO" del Gestor de Carga (GC)
 * - Procesa la lógica de préstamo de libros
 * - Consulta y actualiza el estado en el Gestor de Almacenamiento (GA)
 * - Responde al GC con el resultado de la operación
 * - Comunicación REQ/REP con GA primario y secundario
 * - Failover automático al secundario si primario no responde
 * - Resincronización: cola de updates aplicados al secundario mientras primario estaba caído;
 *   cuando primario vuelve, se re-aplican esos updates para restaurar consistencia.
 *
 * Uso:
 * java ActorPrestamo <gcPubHost> <gcPubPort> <gaHost1> <gaPort1> <gaHost2> <gaPort2> <gcRespHost> <gcRespPort>
 */ 

public class ActorPrestamo {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Configurables:
    private static final int GA_RECV_TIMEOUT_MS = 2000;         // timeout para recv en sockets REQ
    private static final int RESYNC_INTERVAL_SEC = 20;          // cada cuantos segundos intentamos resync
    private static final String PING_ISBN = "__PING__";         // señal se hertbear con los recursos implementados

    // Estructuras de concurrencia
    private static final Object gaLock = new Object();          // lock para acceso a sockets GA (primario/secundario)
    private static final ConcurrentLinkedQueue<String> pendingUpdates = new ConcurrentLinkedQueue<>();
    private static final AtomicReference<ZMQ.Socket> gaPrimRef = new AtomicReference<>();
    private static final AtomicReference<ZMQ.Socket> gaSecRef = new AtomicReference<>();


    public static void main(String[] args) {
        // Validacion de parametros de entrada
        if (args.length < 8) {
            System.out.println("Uso: java ActorPrestamo <gcPubHost> <gcPubPort> <gaHost1> <gaPort1> <gaHost2> <gaPort2> <gcRespHost> <gcRespPort>");
            System.exit(1);
        }

        // Configuración de conexiones
        String gcPubHost = args[0];                 // Host del GC para publicación
        int gcPubPort = Integer.parseInt(args[1]);  // Puerto del GC para publicación
        final String gaHost1 = args[2];                   // Host del GA
        final int gaPort1 = Integer.parseInt(args[3]);    // Puerto del GA
        final String gaHost2 = args[4];                   // Host del GAS
        final int gaPort2 = Integer.parseInt(args[5]);    // Puerto del GAS
        String gcRespHost = args[6];                // Host del GC para respuestas
        int gcRespPort = Integer.parseInt(args[7]); // Puerto del GC para respuestas

        try (ZContext ctx = new ZContext()) {
            // SUB socket: escuchar PRESTAMO del GC
            ZMQ.Socket sub = ctx.createSocket(SocketType.SUB);
            sub.connect("tcp://" + gcPubHost + ":" + gcPubPort);
            sub.subscribe("PRESTAMO".getBytes());
            System.out.println("ActorPrestamo suscrito a PRESTAMO en " + gcPubHost + ":" + gcPubPort);

            // REQ sockets para GA primario y secundario
            synchronized (gaLock) {
                ZMQ.Socket gaPrim = ctx.createSocket(SocketType.REQ);
                gaPrim.setReceiveTimeOut(GA_RECV_TIMEOUT_MS);
                gaPrim.connect("tcp://" + gaHost1 + ":" + gaPort1);
                gaPrimRef.set(gaPrim);
                System.out.println("ActorPrestamo conectado a GA primario " + gaHost1 + ":" + gaPort1);

                ZMQ.Socket gaSec = ctx.createSocket(SocketType.REQ);
                gaSec.setReceiveTimeOut(GA_RECV_TIMEOUT_MS);
                gaSec.connect("tcp://" + gaHost2 + ":" + gaPort2);
                gaSecRef.set(gaSec);
                System.out.println("ActorPrestamo conectado a GA secundario " + gaHost2 + ":" + gaPort2);
            }

            // REQ socket para enviar respuestas al GC (canal de respuestas)
            ZMQ.Socket gcReq = ctx.createSocket(SocketType.REQ);
            gcReq.connect("tcp://" + gcRespHost + ":" + gcRespPort);

            // Scheduler para intentar resincronizar con primario periódicamente
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    intentarResync(ctx, gaHost1, gaPort1);
                } catch (Exception e) {
                    System.out.println("Error en resync: " + e.getMessage());
                }
            }, RESYNC_INTERVAL_SEC, RESYNC_INTERVAL_SEC, TimeUnit.SECONDS);

            // Bucle principal de procesamiento
            while (!Thread.currentThread().isInterrupted()) {
                String topic = sub.recvStr();
                if (topic == null) break;
                String msg = sub.recvStr();
                if (msg == null) break;

                System.out.println("ActorPrestamo recibió: " + msg);
                JsonObject solicitud = JsonParser.parseString(msg).getAsJsonObject();
                String isbn = solicitud.get("isbn").getAsString();
                String usuario = solicitud.get("usuario").getAsString();

                // GET_LIBRO (failover)
                JsonObject petGet = new JsonObject();
                petGet.addProperty("type", "GET_LIBRO");
                petGet.addProperty("isbn", isbn);
                JsonObject respGA = requestConFallback(ctx, petGet, gaHost1, gaPort1);

                if (!respGA.get("estatus").getAsString().equals("OK")) {
                    gcReq.send(respGA.toString());
                    gcReq.recvStr(); // ACK
                    continue;
                }

                JsonObject libro = respGA.getAsJsonObject("libro");
                int copiasDisponibles = libro.get("copiasDisponibles").getAsInt();
                JsonArray prestadoA = libro.getAsJsonArray("prestadoA");
                JsonObject renovaciones = libro.getAsJsonObject("renovaciones");
                JsonObject fechaLim = libro.getAsJsonObject("fechaLim");

                JsonObject resultado = new JsonObject();

                if (copiasDisponibles <= 0) {
                    resultado.addProperty("estatus", "ERROR");
                    resultado.addProperty("mensaje", "No hay copias disponibles");
                } else {
                    // aplicar cambios en memoria local del JSON
                    copiasDisponibles -= 1;
                    libro.addProperty("copiasDisponibles", copiasDisponibles);
                    prestadoA.add(usuario);
                    libro.add("prestadoA", prestadoA);
                    fechaLim.addProperty(usuario, LocalDate.now().plusDays(7).toString());
                    renovaciones.addProperty(usuario, 0);
                    libro.add("fechaLim", fechaLim);
                    libro.add("renovaciones", renovaciones);

                    JsonObject update = new JsonObject();
                    update.addProperty("type", "UPDATE_LIBRO");
                    update.add("libro", libro);

                    JsonObject updateResp = requestConFallback(ctx, update, gaHost1, gaPort1);

                    if (!updateResp.get("estatus").getAsString().equals("OK")) {
                        resultado.addProperty("estatus", "ERROR");
                        resultado.addProperty("mensaje", "No se pudo persistir el préstamo en GA (ambos fallaron)");
                    } else {
                        resultado.addProperty("estatus", "OK");
                        resultado.addProperty("mensaje", "Préstamo registrado");
                        resultado.add("libro", libro);
                    }
                }

                // Responder al Gestor de Carga
                gcReq.send(resultado.toString());
                gcReq.recvStr(); // ACK del GC
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
     * Requiere: gaPrimRef, gagaSecR, pendingUpdates, gaLock y ZContext ctx (si recreas sockets).
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

            // 2) Primario no respondió, intentamos SECUNDARIO
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


        // 3) ni PRIMARIO ni SECUNDARIO respondieron -> devolver ERROR y NO encolar
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
