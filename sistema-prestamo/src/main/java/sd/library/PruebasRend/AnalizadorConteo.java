/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 8 de Octubre 2025
#     Materia: Sistemas Distribuidos
**************************************************************/
package sd.library.PruebasRend;

import java.io.*;
import java.nio.file.*;

/*
 * Procesa y consolida conteos de solicitudes procesadas
 * - Agrega totales de solicitudes de múltiples instancias PSRend
 * - Calcula promedios por proceso solicitante
 * - Genera métricas de throughput del sistema
 * 
 * Metricas calculadas:
 * - ArchivosEncontrados: Número de instancias PSRend activas
 * - SolicitudesTotales: Throughput agregado del sistema
 * - PromedioPorPS: Distribución de carga entre instancias
 */
public class AnalizadorConteo {
    public static void main(String[] args) throws IOException {
        int total = 0;      // Acumulador de todas las solicitudes
        int archivos = 0;   // Contador de instancias PSRend

        // Busqueda y procesamiento de archivos de conteo
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("."), "count_ps_*.txt")) {
            for (Path p : stream) {
                try {
                    int n = Integer.parseInt(Files.readString(p).trim());
                    total += n;     // Acumula al total general
                    archivos++;     // Incrementa contador de instancias
                } catch (Exception ignored) {
                    // Ignora archivos con formato invalido y Mantiene la integridad del procesamiento
                }
            }
        }

        // Manejo de casos sin datos
        if (archivos == 0) {
            System.out.println("ArchivosEncontrados=0");
            System.out.println("SolicitudesTotales=0");
            System.out.println("PromedioPorPS=0");
            return;
        }

        // Calculo de promedio por cada proceso solicitante
        double promedioPorPS = total / (double) archivos;

        System.out.println("ArchivosEncontrados=" + archivos);
        System.out.println("SolicitudesTotales=" + total);
        System.out.println("PromedioPorPS=" + String.format("%.2f", promedioPorPS));
    }
}