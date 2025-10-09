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


import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;


/**
* Proceso Solicitante (PS): lee archivo de peticiones y las envía al Gestor de Carga (GC).
* Uso: java ProcessSolicitante <requestsFile> <gcHost> <gcPort>
*/
public class ProcesoSolicitante {
    private static final Gson gson = new Gson();


    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: java ProcesoSolicitante <Archivo de requests> <gcHost> <gcPort>");
            System.exit(1);
        }
        String file = args[0];
        String gcHost = args[1];
        int gcPort = Integer.parseInt(args[2]);


        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket req = ctx.createSocket(SocketType.REQ);
            req.connect("tcp://" + gcHost + ":" + gcPort);


            try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) 
                        continue;
                    String[] parts = line.split(",");
                    String tipo = parts[0].trim().toUpperCase(Locale.ROOT);
                    String isbn = parts[1].trim();
                    String usuario = parts.length > 2 ? parts[2].trim() : "DESCONOCIDO";
                    Request.Tipo t = Request.Tipo.valueOf(tipo);
                    Request r = new Request(t, isbn, usuario);
                    String json = gson.toJson(r);
                    System.out.println("PS -> GC: " + json);
                    req.send(json);
                    String resp = req.recvStr(0);
                    System.out.println("GC -> PS: " + resp);


                    // pequeña pausa
                    Thread.sleep(200);
                }
            }
        }
    }
}