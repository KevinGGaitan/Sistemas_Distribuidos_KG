/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
**************************************************************/
package sd.library;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.time.LocalDate;

/*
 * Actor especializado que procesa RENOVACION y DEVOLUCION.
 * - Suscripción múltiple a topics "RENOVACION" y "DEVOLUCION"
 * - Procesa lógica de renovación y devolución de libros
 * - Gesta límites de renovación (máximo 2 por usuario)
 * - Coordina con GA para actualizar estado
 */
public class ActorSuscriptor {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Uso: java ActorSuscriptor <gcPubHost> <gcPubPort> <gaHost> <gaPort> <gcRespHost> <gcRespPort>");
            System.exit(1);
        }

        // Configuración de endpoints
        String gcPubHost = args[0];
        int gcPubPort = Integer.parseInt(args[1]);
        String gaHost = args[2];
        int gaPort = Integer.parseInt(args[3]);
        String gcRespHost = args[4];
        int gcRespPort = Integer.parseInt(args[5]);

        try (ZContext ctx = new ZContext()) {
            // SOCKET SUB Suscripción múltiple a dos topics
            ZMQ.Socket sub = ctx.createSocket(SocketType.SUB);
            sub.connect("tcp://" + gcPubHost + ":" + gcPubPort);
            sub.subscribe("RENOVACION".getBytes());
            sub.subscribe("DEVOLUCION".getBytes());
            System.out.println("ActorSuscriptor suscrito a RENOVACION y DEVOLUCION en " + gcPubHost + ":" + gcPubPort);

            // SOCKET REQ Comunicación con GA
            ZMQ.Socket gaReq = ctx.createSocket(SocketType.REQ);
            gaReq.connect("tcp://" + gaHost + ":" + gaPort);

            // SOCKET REQ Comunicación con GC para respuestas
            ZMQ.Socket gcReq = ctx.createSocket(SocketType.REQ);
            gcReq.connect("tcp://" + gcRespHost + ":" + gcRespPort);

            while (!Thread.currentThread().isInterrupted()) {
                // Recepción de mensaje
                String topic = sub.recvStr(); // Identifica tipo de operación
                String msg = sub.recvStr();   // Contenido de la solicitud
                System.out.println("ActorSuscriptor recibió [" + topic + "]: " + msg);

                // Lectura de la solicitud
                JsonObject solicitud = JsonParser.parseString(msg).getAsJsonObject();
                String isbn = solicitud.get("isbn").getAsString();
                String usuario = solicitud.get("usuario").getAsString();

                // Obtener información actual del libro desde GA
                JsonObject getReq = new JsonObject();
                getReq.addProperty("type", "GET_LIBRO");
                getReq.addProperty("isbn", isbn);
                gaReq.send(getReq.toString());

                JsonObject respGA = JsonParser.parseString(gaReq.recvStr()).getAsJsonObject();
                if (!respGA.get("estatus").getAsString().equals("OK")) {
                    // Si hay error, propagar al GC
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

                // Procesamiento segun el tipo de operacion
                if (topic.equals("DEVOLUCION")) {
                    // Verificar que el usuario tenga el libro prestado
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
                        
                        // Remover usuario de la lista de prestados
                        JsonArray nuevaLista = new JsonArray();
                        for (JsonElement e : prestadoA) {
                            if (!e.getAsString().equals(usuario))
                                nuevaLista.add(e);
                        }
                        libro.add("prestadoA", nuevaLista);

                        // Limpiar metadatos del usuario
                        renovaciones.remove(usuario);
                        fechaLim.remove(usuario);
                        libro.add("renovaciones", renovaciones);
                        libro.add("fechaLim", fechaLim);

                        // Incrementar copias disponibles
                        libro.addProperty("copiasDisponibles", copiasDisponibles + 1);

                        // Actualizar en GA
                        JsonObject update = new JsonObject();
                        update.addProperty("type", "UPDATE_LIBRO");
                        update.add("libro", libro);
                        gaReq.send(update.toString());
                        gaReq.recvStr(); // Confirmación

                        resultado.addProperty("estatus", "OK");
                        resultado.addProperty("mensaje", "Devolución exitosa");
                        resultado.add("libro", libro);
                    }
                } else if (topic.equals("RENOVACION")) {
                    // Verificar préstamo activo
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
                        // Verificar límite de renovaciones
                        final int MAX_RENOVACIONES = 2; // Límite de negocio
                        int count = renovaciones.has(usuario) ? renovaciones.get(usuario).getAsInt() : 0;

                        if (count >= MAX_RENOVACIONES) {
                            resultado.addProperty("estatus", "ERROR");
                            resultado.addProperty("mensaje", "Máximo de renovaciones alcanzado");
                        } else {
                            // Realizar renovación
                            
                            // Incrementar contador de renovaciones
                            renovaciones.addProperty(usuario, count + 1);
                            // Extender fecha límite (7 días adicionales)
                            String nuevaFecha = LocalDate.now().plusDays(7).toString();
                            fechaLim.addProperty(usuario, nuevaFecha);

                            // Actualizar estructuras del libro
                            libro.add("renovaciones", renovaciones);
                            libro.add("fechaLim", fechaLim);

                            // Persistir cambios en GA
                            JsonObject update = new JsonObject();
                            update.addProperty("type", "UPDATE_LIBRO");
                            update.add("libro", libro);
                            gaReq.send(update.toString());
                            gaReq.recvStr(); // Confirmación

                            // Preparar respuesta exitosa
                            resultado.addProperty("estatus", "OK");
                            resultado.addProperty("mensaje", "Renovación exitosa (" + (count + 1) + ")");
                            resultado.add("libro", libro);
                        }
                    }
                }

                // Enviar resultado al GC
                gcReq.send(resultado.toString());
                gcReq.recvStr(); // ACK
            }
        }
    }
}