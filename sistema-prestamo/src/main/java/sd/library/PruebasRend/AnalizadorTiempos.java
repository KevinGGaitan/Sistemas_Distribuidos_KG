package sd.library.PruebasRend;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AnalizadorTiempos {
    public static void main(String[] args) throws IOException {
        List<Long> tiempos = new ArrayList<>();

        Files.list(Paths.get("."))
             .filter(p -> p.getFileName().toString().startsWith("times_ps_"))
             .forEach(p -> {
                 try {
                     Files.lines(p).map(Long::parseLong).forEach(tiempos::add);
                 } catch (Exception e) {}
             });

        if (tiempos.isEmpty()) {
            System.out.println("No se encontraron datos de tiempos.");
            return;
        }

        double promedio = tiempos.stream().mapToLong(Long::longValue).average().orElse(0);
        double varianza = tiempos.stream()
                .mapToDouble(t -> Math.pow(t - promedio, 2))
                .average().orElse(0);
        double desv = Math.sqrt(varianza);

        System.out.println("RESULTADOS DEL TEST 1:");
        System.out.println("  - Solicitudes medidas: " + tiempos.size());
        System.out.println("  - Tiempo promedio: " + String.format("%.2f", promedio) + " ms");
        System.out.println("  - Desviación estándar: " + String.format("%.2f", desv) + " ms");
    }
}
