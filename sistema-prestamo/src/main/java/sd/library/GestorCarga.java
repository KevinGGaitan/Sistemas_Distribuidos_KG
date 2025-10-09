/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/
package sd.library;

import com.google.gson.Gson;
import sd.library.model.Request;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;


/**
 * Gestor de Carga (GC): recibe peticiones de PS y las publica por PUB para que los actores las procesen.
 * Uso: java GestorCarga <bindPortPS> <bindPubPort>
 */
public class GestorCarga {
    private static final Gson gson = new Gson();


    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java GestorCarga <bindPortPS> <bindPubPort> <bindRepPort>");
            System.exit(1);
        }

        int bindPortPS = Integer.parseInt(args[0]);   // REQ/REP con PS
        int bindPubPort = Integer.parseInt(args[1]);  // PUB/SUB con actores
        int bindRepPort = Integer.parseInt(args[2]);  // REP con actores (para recibir respuestas)



        try (ZContext ctx = new ZContext()) {
            // REP: canal para PS
            ZMQ.Socket repPS = ctx.createSocket(SocketType.REP);
            repPS.bind("tcp://*:" + bindPortPS);
            System.out.println("GC escuchando PS en puerto " + bindPortPS);

            // PUB: canal para actores
            ZMQ.Socket pub = ctx.createSocket(SocketType.PUB);
            pub.bind("tcp://*:" + bindPubPort);
            System.out.println("GC publicando solicitudes a actores en puerto " + bindPubPort);

            // REP: canal para recibir respuesta del actor
            ZMQ.Socket repActor = ctx.createSocket(SocketType.REP);
            repActor.bind("tcp://*:" + bindRepPort);
            System.out.println("GC esperando respuestas de actores en puerto " + bindRepPort);

            while (!Thread.currentThread().isInterrupted()) {
                //Recibir solicitud del PS
                String reqJson = repPS.recvStr();
                if (reqJson == null) break;
                Request req = gson.fromJson(reqJson, Request.class);
                System.out.println("GC recibió del PS: " + reqJson);

                //Publicar solicitud a los actores
                pub.sendMore(req.getTipo().toString());
                pub.send(reqJson);
                System.out.println("GC publicó solicitud a actores: " + req.getTipo());

                //Esperar respuesta del actor
                System.out.println("GC esperando respuesta del actor...");
                String actorResp = repActor.recvStr();
                System.out.println("GC recibió del actor: " + actorResp);

                //Responder al actor (ACK)
                repActor.send("ACK");

                //Responder al PS
                repPS.send(actorResp);
                System.out.println("GC respondió al PS: " + actorResp);
            }
        }
    }
}
