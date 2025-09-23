
/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 22 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

import java.rmi.Naming;
import java.util.List;
import java.util.Scanner;

public class Cliente {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java Cliente <host>");
            System.exit(1);
        }
        try {
            System.out.println("Buscando Objeto ");
            // Buscar el objeto remoto, la dirección del servidor se pasa como argumento
            String ip = args[0]; // aquí llega la IP desde el make
            Biblioteca biblioteca = (Biblioteca) Naming.lookup("rmi://" + ip + ":1099/MiBiblioteca");

            Scanner sc = new Scanner(System.in);
            System.out.println("=========== COMANDOS ===========");
            System.out.println("| lista                        |");
            System.out.println("| consulta <ISBN>              |");
            System.out.println("| prestamo <ISBN> <usuario>    |");
            System.out.println("| prestamo <titulo> <usuario>  |");
            System.out.println("| renovacion <ISBN> <usuario>  |");
            System.out.println("| devolucion <ISBN> <usuario>  |");
            System.out.println("| salir                        |");
            System.out.println("================================");

            // Invocar método remoto segun la solicitud del cliente
            while (true) {
                System.out.print("> ");
                String line = sc.nextLine().trim();
                if (line.equalsIgnoreCase("salir"))
                    break;
                if (line.equalsIgnoreCase("lista")) {
                    // Invocar método remoto para listar inventario
                    List<Libro> inv = biblioteca.listaInventario();
                    inv.forEach(b -> System.out.println(b.toString()));
                    continue;
                }
                String[] partes = line.split("\\s+");

                if (partes[0].equalsIgnoreCase("consulta")) {
                    if (partes.length < 2) {
                        System.out.println("Comando invalido.");
                        continue;
                    }
                    Libro l = biblioteca.consulta(partes[1]);
                    if (l == null) {
                        System.out.println("No se encontro el libro con ISBN: " + partes[1]);
                    } else {
                        System.out.println(l.toString());
                    }
                    continue;
                }
                if (partes.length < 3) {
                    System.out.println("Comando invalido.");
                    continue;
                }
                String comando = partes[0];
                String isbn = partes[1];
                String usuario = partes[2];
                try {
                    switch (comando.toLowerCase()) {
                        case "prestamo":
                            if (esISBN(isbn)) {
                                // Invocar método remoto de prestamo con isbn
                                Transaccion r1 = biblioteca.prestamo(isbn, usuario);
                                System.out.println(r1.mensaje);
                            } else {
                                // El título puede tener espacios entonces hay que reconstruirlo
                                StringBuilder sb = new StringBuilder();
                                for (int i = 1; i < partes.length - 1; i++) {
                                    if (i > 1)
                                        sb.append(" ");
                                    sb.append(partes[i]);
                                }
                                String titulo = sb.toString();
                                usuario = partes[partes.length - 1];
                                // Invocar método remoto de prestamo con titulo
                                Transaccion r1 = biblioteca.prestamoNombre(titulo, usuario);
                                System.out.println(r1.mensaje);
                            }
                            break;
                        case "renovacion":
                            // Invocar método remoto de renovacion
                            Transaccion r2 = biblioteca.renovacion(isbn, usuario);
                            System.out.println(r2.mensaje);
                            break;
                        case "devolucion":
                            // Invocar método remoto devolucion
                            Transaccion r3 = biblioteca.devolucion(isbn, usuario);
                            System.out.println(r3.mensaje);
                            break;
                        default:
                            System.out.println("Comando desconocido.");
                    }
                } catch (Exception ex) {
                    System.err.println("Error llamando al servidor: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
            sc.close();
        } catch (Exception e) {
            System.err.println("Excepcion del cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean esISBN(String str) {
        if (str == null) {
            return false;
        }
        try {
            Integer.parseInt(str); // Intenta convertir a entero
            return true;
        } catch (NumberFormatException e) {
            return false; // No es un entero válido
        }
    }
}