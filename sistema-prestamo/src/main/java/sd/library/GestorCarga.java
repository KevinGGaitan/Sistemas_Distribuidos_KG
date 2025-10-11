/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
**************************************************************/
package sd.library;

import com.google.gson.Gson;
import sd.library.model.Request;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/*
 * Componente intermediario que coordina el flujo de mensajes
 * - REP: Para recibir solicitudes del PS (Proceso Solicitante)
 * - PUB: Para distribuir solicitudes a actores especializados
 * - REP: Para recibir respuestas de actores
 */
public class GestorCarga {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java GestorCarga <bindPortPS> <bindPubPort> <bindRepPort>");
            System.exit(1);
        }

        int bindPortPS = Integer.parseInt(args[0]);   // REP: Comunicación con PS
        int bindPubPort = Integer.parseInt(args[1]);  // PUB: Distribución a actores
        int bindRepPort = Integer.parseInt(args[2]);  // REP: Respuestas de actores

        try (ZContext ctx = new ZContext()) {
            // SOCKET REP Canal para recibir solicitudes del PS
            ZMQ.Socket repPS = ctx.createSocket(SocketType.REP);
            repPS.bind("tcp://*:" + bindPortPS);
            System.out.println("GC escuchando PS en puerto " + bindPortPS);

            // SOCKET PUB Canal para publicar solicitudes a actores
            ZMQ.Socket pub = ctx.createSocket(SocketType.PUB);
            pub.bind("tcp://*:" + bindPubPort);
            System.out.println("GC publicando solicitudes a actores en puerto " + bindPubPort);

            // SOCKET REP Canal para recibir respuestas de actores
            ZMQ.Socket repActor = ctx.createSocket(SocketType.REP);
            repActor.bind("tcp://*:" + bindRepPort);
            System.out.println("GC esperando respuestas de actores en puerto " + bindRepPort);

            while (!Thread.currentThread().isInterrupted()) {
                // Recibir solicitud del PS
                String reqJson = repPS.recvStr();
                if (reqJson == null) break;
                Request req = gson.fromJson(reqJson, Request.class);
                System.out.println("GC recibió del PS: " + reqJson);

                // Publicar solicitud a actores (patrón PUB/SUB)
                pub.sendMore(req.getTipo().toString()); // Topic (PRESTAMO, DEVOLUCION o RENOVACION)
                pub.send(reqJson);                      // Contenido JSON
                System.out.println("GC publicó solicitud a actores: " + req.getTipo());

                // Esperar respuesta del actor especializado
                System.out.println("GC esperando respuesta del actor...");
                String actorResp = repActor.recvStr();
                System.out.println("GC recibió del actor: " + actorResp);

                // Confirmar recepción al actor
                repActor.send("ACK");

                // Reenviar respuesta al PS
                repPS.send(actorResp);
                System.out.println("GC respondió al PS: " + actorResp);
            }
        }
    }
}