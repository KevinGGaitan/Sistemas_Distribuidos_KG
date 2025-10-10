/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/
package sd.library.PruebasRend;

import com.google.gson.Gson;

import sd.library.model.Request;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


/**
* Proceso Solicitante (PS): lee archivo de peticiones y las envía al Gestor de Carga (GC).
* Uso: java ProcessSolicitante <requestsFile> <gcHost> <gcPort>
*/
public class PSRend {
    private static final Gson gson = new Gson();


    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: java ProcesoSolicitante <ArchivoRequests> <gcHost> <gcPort>");
            System.exit(1);
        }

        String file = args[0];
        String gcHost = args[1];
        int gcPort = Integer.parseInt(args[2]);
        String psId = UUID.randomUUID().toString().substring(0, 4); // ID corto del PS

        List<Long> tiempos = new ArrayList<>();
        int procesadas = 0;
        long inicio = System.currentTimeMillis();
        long duracion = 120_000; // 2 minutos (en ms)

        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket req = ctx.createSocket(SocketType.REQ);
            req.connect("tcp://" + gcHost + ":" + gcPort);
            System.out.println("PS [" + psId + "] conectado a GC en " + gcHost + ":" + gcPort);

            // Cargar solicitudes
            List<String> lineas = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);

            while (System.currentTimeMillis() - inicio < duracion) {
                for (String line : lineas) {
                    if (System.currentTimeMillis() - inicio >= duracion) break;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#"))
                        continue;

                    String[] parts = line.split(",");
                    if (parts.length < 2) continue;
                    String tipo = parts[0].trim().toUpperCase(Locale.ROOT);
                    String isbn = parts[1].trim();
                    String usuario = parts.length > 2 ? parts[2].trim() : "DESCONOCIDO";

                    Request.Tipo t = Request.Tipo.valueOf(tipo);
                    Request r = new Request(t, isbn, usuario);
                    String json = gson.toJson(r);

                    long t0 = System.nanoTime();
                    req.send(json);
                    String resp = req.recvStr(0);
                    long t1 = System.nanoTime();

                    long deltaMs = (t1 - t0) / 1_000_000;
                    tiempos.add(deltaMs);
                    procesadas++;

                    System.out.println("PS [" + psId + "] -> GC: " + json);
                    System.out.println("GC -> PS [" + psId + "]: " + resp);
                }
            }

            System.out.println("PS [" + psId + "] terminó ciclo de 2 minutos.");
        } catch (Exception e) {
            System.err.println("Error en PS [" + psId + "]: " + e.getMessage());
        }

        // Guardar resultados
        try {
            Path timesFile = Paths.get("times_ps_" + psId + ".csv");
            Files.write(timesFile, () -> tiempos.stream().<CharSequence>map(Object::toString).iterator());

            Path countFile = Paths.get("count_ps_" + psId + ".txt");
            Files.writeString(countFile, String.valueOf(procesadas));

            System.out.println("PS [" + psId + "] finalizado.");
            System.out.println("   - Solicitudes procesadas: " + procesadas);
            System.out.println("   - Archivo de tiempos: " + timesFile.getFileName());
            System.out.println("   - Archivo de conteo: " + countFile.getFileName());
        } catch (IOException e) {
            System.err.println("Error al guardar resultados: " + e.getMessage());
        }
    }
}