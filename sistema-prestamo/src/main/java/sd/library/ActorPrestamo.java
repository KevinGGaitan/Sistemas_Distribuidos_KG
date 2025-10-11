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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.time.LocalDate;

/*
 * Actor especializado que escucha exclusivamente solicitudes de tipo PRESTAMO.
 * - Se suscribe al topic "PRESTAMO" del Gestor de Carga (GC)
 * - Procesa la lógica de préstamo de libros
 * - Consulta y actualiza el estado en el Gestor de Almacenamiento (GA)
 * - Responde al GC con el resultado de la operación
 */
public class ActorPrestamo {
    // Configuración de GSON para serialización/deserialización JSON con formato legible
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        // Validación de parámetros de entrada
        if (args.length < 6) {
            System.out.println("Uso: java ActorPrestamo <gcPubHost> <gcPubPort> <gaHost> <gaPort> <gcRespHost> <gcRespPort>");
            System.exit(1);
        }

        // Configuración de conexiones
        String gcPubHost = args[0];      // Host del GC para publicación
        int gcPubPort = Integer.parseInt(args[1]);  // Puerto del GC para publicación
        String gaHost = args[2];         // Host del GA
        int gaPort = Integer.parseInt(args[3]);     // Puerto del GA
        String gcRespHost = args[4];     // Host del GC para respuestas
        int gcRespPort = Integer.parseInt(args[5]); // Puerto del GC para respuestas

        try (ZContext ctx = new ZContext()) {
            // SOCKET SUB Para suscribirse a mensajes del GC
            ZMQ.Socket sub = ctx.createSocket(SocketType.SUB);
            sub.connect("tcp://" + gcPubHost + ":" + gcPubPort);
            sub.subscribe("PRESTAMO".getBytes());  // Filtro: solo mensajes PRESTAMO
            System.out.println("ActorPrestamo suscrito a PRESTAMO en " + gcPubHost + ":" + gcPubPort);

            // SOCKET REQ Para consultar/actualizar el GA
            ZMQ.Socket gaReq = ctx.createSocket(SocketType.REQ);
            gaReq.connect("tcp://" + gaHost + ":" + gaPort);

            // SOCKET REQ Para enviar respuestas al GC
            ZMQ.Socket gcReq = ctx.createSocket(SocketType.REQ);
            gcReq.connect("tcp://" + gcRespHost + ":" + gcRespPort);

            // Bucle principal de procesamiento
            while (!Thread.currentThread().isInterrupted()) {
                // Recepción de mensaje (formato: topic + contenido)
                String topic = sub.recvStr(); // "PRESTAMO"
                String msg = sub.recvStr();   // Contenido JSON
                System.out.println("ActorPrestamo recibió: " + msg);
                
                // Lectura del JSON de solicitud
                JsonObject solicitud = JsonParser.parseString(msg).getAsJsonObject();
                String isbn = solicitud.get("isbn").getAsString();
                String usuario = solicitud.get("usuario").getAsString();

                // Consultar libro al Gestor de Almacenamiento
                JsonObject peticion = new JsonObject();
                peticion.addProperty("type", "GET_LIBRO");
                peticion.addProperty("isbn", isbn);
                gaReq.send(peticion.toString());

                // Procesar respuesta del GA
                JsonObject respGA = JsonParser.parseString(gaReq.recvStr()).getAsJsonObject();
                if (!respGA.get("estatus").getAsString().equals("OK")) {
                    // Si hay error, reenviar al GC y continuar
                    gcReq.send(respGA.toString());
                    gcReq.recvStr(); // ACK del GC
                    continue;
                }

                // Extraer datos del libro desde la respuesta
                JsonObject libro = respGA.getAsJsonObject("libro");
                int copiasDisponibles = libro.get("copiasDisponibles").getAsInt();
                JsonArray prestadoA = libro.getAsJsonArray("prestadoA");
                JsonObject renovaciones = libro.getAsJsonObject("renovaciones");
                JsonObject fechaLim = libro.getAsJsonObject("fechaLim");

                JsonObject resultado = new JsonObject();

                // Verificar disponibilidad
                if (copiasDisponibles <= 0) {
                    resultado.addProperty("estatus", "ERROR");
                    resultado.addProperty("mensaje", "No hay copias disponibles");
                } else {
                    // Realizar préstamo
                    
                    // Reducir copias disponibles
                    copiasDisponibles -= 1;
                    libro.addProperty("copiasDisponibles", copiasDisponibles);

                    // Agregar usuario a la lista de prestados
                    prestadoA.add(usuario);
                    libro.add("prestadoA", prestadoA);

                    // Establecer fecha límite (7 días desde hoy)
                    fechaLim.addProperty(usuario, LocalDate.now().plusDays(7).toString());
                    // Inicializar contador de renovaciones
                    renovaciones.addProperty(usuario, 0);

                    // Actualizar estructuras en el objeto libro
                    libro.add("fechaLim", fechaLim);
                    libro.add("renovaciones", renovaciones);

                    // Enviar actualización al GA
                    JsonObject update = new JsonObject();
                    update.addProperty("type", "UPDATE_LIBRO");
                    update.add("libro", libro);
                    gaReq.send(update.toString());
                    gaReq.recvStr(); // Confirmación del GA

                    // Preparar respuesta exitosa
                    resultado.addProperty("estatus", "OK");
                    resultado.addProperty("mensaje", "Préstamo registrado");
                    resultado.add("libro", libro);
                }

                // Responder al Gestor de Carga
                gcReq.send(resultado.toString());
                gcReq.recvStr(); // ACK del GC
            }
        }
    }
}