/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 8 de Octubre 2025
#     Materia: Sistemas Distribuidos
**************************************************************/
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

/*
 * Proceso Solicitante Especializado para Pruebas de Rendimiento
 * 
 * Diferencias con ProcesoSolicitante regulas:
 * - Ejecución por tiempo fijo (2 minutos) en lugar de por archivo
 * - Medición precisa de tiempos de respuesta
 * - Generación de archivos de métricas
 * - Identificador único por instancia
 * - Ciclo continuo de solicitudes
 * 
 * Metricas medidas:
 * - Tiempo de respuesta por solicitud (nanosegundos → milisegundos)
 * - Cantidad total de solicitudes procesadas
 * - Archivos de salida para análisis posterior
 */
public class PSRend {
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: java ProcesoSolicitante <ArchivoRequests> <gcHost> <gcPort>");
            System.exit(1);
        }

        // Configuracion inicial
        String file = args[0];                    // Archivo con solicitudes de prueba
        String gcHost = args[1];                  // Host del Gestor de Carga
        int gcPort = Integer.parseInt(args[2]);   // Puerto del Gestor de Carga
        String psId = UUID.randomUUID().toString().substring(0, 4); // ID único de 4 caracteres

        // Estructura para capturar metricas
        List<Long> tiempos = new ArrayList<>();   // Almacena todos los tiempos de respuesta
        int procesadas = 0;                       // Contador de solicitudes procesadas
        long inicio = System.currentTimeMillis(); // Marca de tiempo inicial
        long duracion = 120_000;                  // Duración total: 2 minutos en milisegundos

        try (ZContext ctx = new ZContext()) {
            // Configuracion SOCKET ZEROMQ
            ZMQ.Socket req = ctx.createSocket(SocketType.REQ); // Socket REQ para comunicación sincrónica
            req.connect("tcp://" + gcHost + ":" + gcPort);
            System.out.println("PS [" + psId + "] conectado a GC en " + gcHost + ":" + gcPort);

            // Carga de solicitudes desde achivo
            // Lee todas las líneas del archivo para reutilización en ciclo
            List<String> lineas = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);

            // Ejecucion por tiempo, continúa ejecutando hasta que se cumplan 2 minutos
            while (System.currentTimeMillis() - inicio < duracion) {
                // Iteracion sobre las solicitudes
                for (String line : lineas) {
                    // Verificar el tiempo trascurrido, salir si se excede duración
                    if (System.currentTimeMillis() - inicio >= duracion) break;
                    
                    line = line.trim();
                    // Saltar líneas vacías o comentarios
                    if (line.isEmpty() || line.startsWith("#"))
                        continue;

                    // Lectura de lineas de solicitud
                    String[] parts = line.split(",");
                    if (parts.length < 2) continue; // Validación formato mínimo
                    
                    String tipo = parts[0].trim().toUpperCase(Locale.ROOT);
                    String isbn = parts[1].trim();
                    String usuario = parts.length > 2 ? parts[2].trim() : "DESCONOCIDO";

                    // Creacion de Request
                    Request.Tipo t = Request.Tipo.valueOf(tipo);
                    Request r = new Request(t, isbn, usuario);
                    String json = gson.toJson(r);

                    // Medicion de tiempo de respuesta
                    long t0 = System.nanoTime();  // Timestamp antes de enviar
                    req.send(json);               // Envío de solicitud
                    String resp = req.recvStr(0); // Recepción de respuesta (0 = no wait)
                    long t1 = System.nanoTime();  // Timestamp después de recibir

                    // Calcula y almacena la metrica
                    long deltaMs = (t1 - t0) / 1_000_000; // Conversión nanosegundos → milisegundos
                    tiempos.add(deltaMs);         // Agregar a lista de tiempos
                    procesadas++;                 // Incrementar contador

                    // Log de Actividad
                    System.out.println("PS [" + psId + "] -> GC: " + json);
                    System.out.println("GC -> PS [" + psId + "]: " + resp);
                }
            }

            // Finalizacion controlada
            System.out.println("PS [" + psId + "] terminó ciclo de 2 minutos.");
        } catch (Exception e) {
            // Manejo de errores
            System.err.println("Error en PS [" + psId + "]: " + e.getMessage());
        }

        // Persistencia de resultados
        try {
            // Archivo de tiempos
            Path timesFile = Paths.get("times_ps_" + psId + ".csv");
            Files.write(timesFile, () -> tiempos.stream().<CharSequence>map(Object::toString).iterator());

            // Archivo de conteos
            Path countFile = Paths.get("count_ps_" + psId + ".txt");
            Files.writeString(countFile, String.valueOf(procesadas));

            // Reporte final
            System.out.println("PS [" + psId + "] finalizado.");
            System.out.println("   - Solicitudes procesadas: " + procesadas);
            System.out.println("   - Archivo de tiempos: " + timesFile.getFileName());
            System.out.println("   - Archivo de conteo: " + countFile.getFileName());
        } catch (IOException e) {
            System.err.println("Error al guardar resultados: " + e.getMessage());
        }
    }
}