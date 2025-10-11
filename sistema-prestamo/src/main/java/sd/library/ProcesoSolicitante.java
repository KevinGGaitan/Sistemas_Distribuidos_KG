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

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Cliente que genera carga en el sistema
 * - Lee solicitudes desde archivo de texto
 * - Las envía secuencialmente al GC
 * - Espera y muestra respuestas
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
            // SOCKET REQ Conexión sincrónica al GC
            ZMQ.Socket req = ctx.createSocket(SocketType.REQ);
            req.connect("tcp://" + gcHost + ":" + gcPort);

            // Lectura del archivo de solicitudes
            try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    // Saltar líneas vacías o comentarios
                    if (line.isEmpty() || line.startsWith("#")) 
                        continue;
                    
                    // Lectura de línea: TIPO,ISBN,USUARIO
                    String[] parts = line.split(",");
                    String tipo = parts[0].trim().toUpperCase(Locale.ROOT);
                    String isbn = parts[1].trim();
                    String usuario = parts.length > 2 ? parts[2].trim() : "DESCONOCIDO";
                    
                    // Crear objeto Request
                    Request.Tipo t = Request.Tipo.valueOf(tipo);
                    Request r = new Request(t, isbn, usuario);
                    String json = gson.toJson(r);
                    
                    // Envío y recepción sincrónica
                    System.out.println("PS -> GC: " + json);
                    req.send(json);
                    String resp = req.recvStr(0); // 0 = sin espera
                    System.out.println("GC -> PS: " + resp);

                    // Pequeña pausa para evitar saturación
                    Thread.sleep(200);
                }
            }
        }
    }
}