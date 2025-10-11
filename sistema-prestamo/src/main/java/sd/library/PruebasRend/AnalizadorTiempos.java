/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 8 de Octubre 2025
#     Materia: Sistemas Distribuidos
**************************************************************/
package sd.library.PruebasRend;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/*
 * Procesa y analiza archivos de tiempos de respuesta
 * - Agrega todos los tiempos de respuesta de múltiples instancias PSRend
 * - Calcula estadísticas descriptivas:
 *   - Tiempo promedio de respuesta
 *   - Desviación estándar (variabilidad)
 *   - Cantidad total de mediciones
 * 
 * Metodos de calculo
 * - Promedio: medida de tendencia central
 * - Varianza: promedio de las diferencias cuadráticas con la media
 * - Desviación estándar: raíz cuadrada de la varianza
 */
public class AnalizadorTiempos {
    public static void main(String[] args) throws IOException {
        // Agrupar todos los tiempos
        List<Long> tiempos = new ArrayList<>();

        // Encuentra todos los archivos CSV de tiempos
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("."), "times_ps_*.csv")) {
            for (Path p : stream) {
                try {
                    Files.lines(p).map(Long::parseLong).forEach(tiempos::add);
                } catch (Exception ignored) {
                    // Ignora archivos con formato invalido y permite continuar con otros archivos válidos
                }
            }
        }

        // Manejo de caso sin datos
        if (tiempos.isEmpty()) {
            System.out.println("SolicitudesMedidas=0");
            System.out.println("TiempoPromedio=0");
            System.out.println("DesviacionEstandar=0");
            return;
        }

        // Calculo de estadisticas
        
        // PROMEDIO: Suma de todos los valores / cantidad de valores
        double promedio = tiempos.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        // VARIANZA: Medida de dispersión de los datos
        double varianza = tiempos.stream()
                .mapToDouble(t -> Math.pow(t - promedio, 2))  
                .average()                                    
                .orElse(0);                                  

        // DESVIACIÓN ESTÁNDAR: Raíz cuadrada de la varianza
        double desv = Math.sqrt(varianza);

        System.out.println("SolicitudesMedidas=" + tiempos.size());
        System.out.println("TiempoPromedio=" + String.format("%.2f", promedio));
        System.out.println("DesviacionEstandar=" + String.format("%.2f", desv));
    }
}