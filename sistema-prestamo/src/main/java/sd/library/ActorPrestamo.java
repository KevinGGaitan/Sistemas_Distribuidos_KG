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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.time.LocalDate;
/**
 * ActorPrestamo: escucha las solicitudes PRESTAMO, procesa la lógica y envía resultado al GA.
 * Uso: java ActorPrestamo <gcHost> <gcPubPort> <gaHost> <gaPort>
 */
public class ActorPrestamo {
   private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Uso: java ActorPrestamo <gcPubHost> <gcPubPort> <gaHost> <gaPort> <gcRespHost> <gcRespPort>");
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
            sub.subscribe("PRESTAMO".getBytes());
            System.out.println("ActorPrestamo suscrito a PRESTAMO en " + gcPubHost + ":" + gcPubPort);

            ZMQ.Socket gaReq = ctx.createSocket(SocketType.REQ);
            gaReq.connect("tcp://" + gaHost + ":" + gaPort);

            ZMQ.Socket gcReq = ctx.createSocket(SocketType.REQ);
            gcReq.connect("tcp://" + gcRespHost + ":" + gcRespPort);

            while (!Thread.currentThread().isInterrupted()) {
                String topic = sub.recvStr(); // "PRESTAMO"
                String msg = sub.recvStr();
                System.out.println("ActorPrestamo recibió: " + msg);
                JsonObject solicitud = JsonParser.parseString(msg).getAsJsonObject();
                String isbn = solicitud.get("isbn").getAsString();
                String usuario = solicitud.get("usuario").getAsString();

                // Paso 1: pedir libro al GA
                JsonObject peticion = new JsonObject();
                peticion.addProperty("type", "GET_LIBRO");
                peticion.addProperty("isbn", isbn);
                gaReq.send(peticion.toString());

                JsonObject respGA = JsonParser.parseString(gaReq.recvStr()).getAsJsonObject();
                if (!respGA.get("estatus").getAsString().equals("OK")) {
                    gcReq.send(respGA.toString());
                    gcReq.recvStr();
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
                    // Actualizar datos
                    copiasDisponibles -= 1;
                    libro.addProperty("copiasDisponibles", copiasDisponibles);

                    // Agregar usuario
                    prestadoA.add(usuario);
                    libro.add("prestadoA", prestadoA);

                    // Añadir fecha y renovaciones
                    fechaLim.addProperty(usuario, LocalDate.now().plusDays(7).toString());
                    renovaciones.addProperty(usuario, 0);

                    libro.add("fechaLim", fechaLim);
                    libro.add("renovaciones", renovaciones);


                    // Enviar actualización
                    JsonObject update = new JsonObject();
                    update.addProperty("type", "UPDATE_LIBRO");
                    update.add("libro", libro);
                    gaReq.send(update.toString());
                    gaReq.recvStr();

                    resultado.addProperty("estatus", "OK");
                    resultado.addProperty("mensaje", "Préstamo registrado");
                    resultado.add("libro", libro);
                }

                // Responder al GC
                gcReq.send(resultado.toString());
                gcReq.recvStr();
            }
        }
    }
}