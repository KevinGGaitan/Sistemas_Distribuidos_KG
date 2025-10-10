package sd.library.PruebasRend;

import java.io.*;
import java.nio.file.*;

public class AnalizadorConteo {
    public static void main(String[] args) throws IOException {
        int total = 0;
        int archivos = 0;

        for (Path p : Files.newDirectoryStream(Paths.get("."), "count_ps_*.txt")) {
            try {
                int n = Integer.parseInt(Files.readString(p).trim());
                total += n;
                archivos++;
            } catch (Exception ignored) {}
        }

        if (archivos == 0) {
            System.out.println("No se encontraron archivos de conteo.");
            return;
        }

        System.out.println("RESULTADOS DEL TEST 2:");
        System.out.println("  - PS simultáneos: " + archivos);
        System.out.println("  - Total de solicitudes procesadas: " + total);
        System.out.println("  - Duración total: 2 minutos");
        System.out.println("  - Promedio por PS: " + (total / (double) archivos));
    }
}
