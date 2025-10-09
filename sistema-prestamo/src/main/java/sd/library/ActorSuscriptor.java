/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/
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

/**
 * ActorSubscriber: escucha DEVOLUCION y RENOVACION, procesa la lógica y envía resultado al GA.
 * Uso: java ActorSubscriber <gcHost> <gcPubPort> <gaHost> <gaPort>
 */
public class ActorSuscriptor {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Uso: java ActorSuscriptor <gcPubHost> <gcPubPort> <gaHost> <gaPort> <gcRespHost> <gcRespPort>");
            System.exit(1);
        }

        String gcPubHost = args[0];
        int gcPubPort = Integer.parseInt(args[1]);
        String gaHost = args[2];
        int gaPort = Integer.parseInt(args[3]);
        String gcRespHost = args[4];
        int gcRespPort = Integer.parseInt(args[5]);


        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket sub = ctx.createSocket(SocketType.SUB);
            sub.connect("tcp://" + gcPubHost + ":" + gcPubPort);
            sub.subscribe("RENOVACION".getBytes());
            sub.subscribe("DEVOLUCION".getBytes());
            System.out.println("ActorSuscriptor suscrito a RENOVACION y DEVOLUCION en " + gcPubHost + ":" + gcPubPort);

            ZMQ.Socket gaReq = ctx.createSocket(SocketType.REQ);
            gaReq.connect("tcp://" + gaHost + ":" + gaPort);

            ZMQ.Socket gcReq = ctx.createSocket(SocketType.REQ);
            gcReq.connect("tcp://" + gcRespHost + ":" + gcRespPort);


            while (!Thread.currentThread().isInterrupted()) {
                String topic = sub.recvStr(); // "RENOVACION" o "DEVOLUCION"
                String msg = sub.recvStr();
                System.out.println("ActorSuscriptor recibió [" + topic + "]: " + msg);

                JsonObject solicitud = JsonParser.parseString(msg).getAsJsonObject();
                String isbn = solicitud.get("isbn").getAsString();
                String usuario = solicitud.get("usuario").getAsString();

                // Paso 1: obtener libro
                JsonObject getReq = new JsonObject();
                getReq.addProperty("type", "GET_LIBRO");
                getReq.addProperty("isbn", isbn);
                gaReq.send(getReq.toString());

                JsonObject respGA = JsonParser.parseString(gaReq.recvStr()).getAsJsonObject();
                if (!respGA.get("estatus").getAsString().equals("OK")) {
                    gcReq.send(respGA.toString());
                    gcReq.recvStr();
                    continue;
                }

                JsonObject libro = respGA.getAsJsonObject("libro");
                JsonArray prestadoA = libro.getAsJsonArray("prestadoA");
                JsonObject renovaciones = libro.getAsJsonObject("renovaciones");
                JsonObject fechaLim = libro.getAsJsonObject("fechaLim");
                int copiasDisponibles = libro.get("copiasDisponibles").getAsInt();

                JsonObject resultado = new JsonObject();

                if (topic.equals("DEVOLUCION")) {
                    // Verificar si el usuario tiene el libro
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
                        // Remover usuario
                        JsonArray nuevaLista = new JsonArray();
                        for (JsonElement e : prestadoA) {
                            if (!e.getAsString().equals(usuario))
                                nuevaLista.add(e);
                        }
                        libro.add("prestadoA", nuevaLista);

                        renovaciones.remove(usuario);
                        fechaLim.remove(usuario);
                        libro.add("renovaciones", renovaciones);
                        libro.add("fechaLim", fechaLim);

                        libro.addProperty("copiasDisponibles", copiasDisponibles + 1);

                        // Enviar update
                        JsonObject update = new JsonObject();
                        update.addProperty("type", "UPDATE_LIBRO");
                        update.add("libro", libro);
                        gaReq.send(update.toString());
                        gaReq.recvStr();

                        resultado.addProperty("estatus", "OK");
                        resultado.addProperty("mensaje", "Devolución exitosa");
                        resultado.add("libro", libro);
                    }
                } else if (topic.equals("RENOVACION")) {
                    // Verificar si el usuario lo tiene prestado
                    boolean tiene = false;
                    for (JsonElement e : prestadoA) {
                        if (e.getAsString().equals(usuario)) {
                            tiene = true;
                            break;
                        }
                    }

                    if (!tiene) {
                        //El usuario no tiene el libro
                        resultado.addProperty("estatus", "ERROR");
                        resultado.addProperty("mensaje", "El usuario no tiene este libro para renovar");
                    } else {
                        //Tiene el libro, validar número de renovaciones
                        final int MAX_RENOVACIONES = 2; // límite permitido
                        int count = renovaciones.has(usuario) ? renovaciones.get(usuario).getAsInt() : 0;

                        if (count >= MAX_RENOVACIONES) {
                            //Ya alcanzó el máximo de renovaciones
                            resultado.addProperty("estatus", "ERROR");
                            resultado.addProperty("mensaje", "Máximo de renovaciones alcanzado");
                        } else {
                            //Puede renovar
                            renovaciones.addProperty(usuario, count + 1);
                            // Nueva fecha límite: 7 días más
                            String nuevaFecha = LocalDate.now().plusDays(7).toString();
                            fechaLim.addProperty(usuario, nuevaFecha);

                            // Actualizar estructuras del libro
                            libro.add("renovaciones", renovaciones);
                            libro.add("fechaLim", fechaLim);

                            // Enviar al GA para que actualice el inventario
                            JsonObject update = new JsonObject();
                            update.addProperty("type", "UPDATE_LIBRO");
                            update.add("libro", libro);
                            gaReq.send(update.toString());
                            gaReq.recvStr(); // esperar confirmación del GA

                            // Responder al GC
                            resultado.addProperty("estatus", "OK");
                            resultado.addProperty("mensaje", "Renovación exitosa (" + (count + 1) + ")");
                            resultado.add("libro", libro);
                        }
                    }
                }

                gcReq.send(resultado.toString());
                gcReq.recvStr();
            }
        }
    }
}