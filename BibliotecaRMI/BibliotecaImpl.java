
/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 22 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.*;
import java.io.IOException;
import java.net.InetAddress;

// Implementación de la interfaz Biblioteca
public class BibliotecaImpl extends UnicastRemoteObject implements Biblioteca {
    private final Map<String, Libro> inventario = new ConcurrentHashMap<>();
    private final Map<String, Map<String, InfoDevolucion>> prestamos = new ConcurrentHashMap<>();

    // Duracion de los prestamos y cantidad maxima de renovaciones (configurable)
    private final int DIAS_PRESTAMO = 14;
    private final int RENOVACION_MAX = 2;

     // Constructor simple, sin rebind
    public BibliotecaImpl() throws RemoteException {
        super();
    }

    // Cargar inventario de archivo txt pasadp como argumento. formato:
    // isbn,titulo,autor,copias
    public void cargarInventario(String path) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(path));
        for (String linea : lineas) {
            if (linea.trim().isEmpty() || linea.startsWith("#"))
                continue;
            String[] parts = linea.split(",", 4);
            if (parts.length < 4)
                continue;
            String isbn = parts[0].trim();
            String titulo = parts[1].trim();
            String autor = parts[2].trim();
            int copias = Integer.parseInt(parts[3].trim());
            inventario.put(isbn, new Libro(isbn, titulo, autor, copias));
        }
        System.out.println("inventario cargado. libros: " + inventario.size());
    }

    @Override
    public Transaccion prestamo(String isbn, String usuario) throws RemoteException {
        Libro libro = inventario.get(isbn);
        if (libro == null)
            return new Transaccion(false, "ISBN no encontrado: " + isbn);

        // Obtener o crear mapa de prestamos por ISBN
        prestamos.putIfAbsent(isbn, new ConcurrentHashMap<>());
        Map<String, InfoDevolucion> map = prestamos.get(isbn);

        // Prestamo de un libro
        synchronized (libro) { // Manejo de hilos, blockeo para limitar las operaciones de Libros a un solo
                               // hilo
            if (map.containsKey(usuario)) {
                return new Transaccion(false, "Usuario ya tiene prestado este libro (ISBN: " + isbn + ")");
            }
            if (!libro.prestar()) {
                return new Transaccion(false, "No hay copias disponibles para ISBN: " + isbn);
            }
            LocalDate fechaLim = LocalDate.now().plusDays(DIAS_PRESTAMO);
            InfoDevolucion info = new InfoDevolucion(usuario, fechaLim);
            map.put(usuario, info);
            String mensaje = String.format("Prestamo exitoso. ISBN: %s, usuario: %s, vence: %s", isbn, usuario,
                    fechaLim);
            return new Transaccion(true, mensaje, fechaLim);
        }
    }

    @Override
    public Transaccion prestamoNombre(String titulo, String usuario) throws RemoteException {
        // Buscar libro con titulo que coincida
        for (Libro libro : inventario.values()) {
            if (libro.titulo.equalsIgnoreCase(titulo)) {
                // Reutilizamos logica de prestamo por ISBN
                return prestamo(libro.isbn, usuario);
            }
        }
        return new Transaccion(false, "No se encontro libro con titulo: " + titulo);
    }

    @Override
    public Transaccion renovacion(String isbn, String usuario) throws RemoteException {
        Libro libro = inventario.get(isbn);
        if (libro == null)
            return new Transaccion(false, "ISBN no encontrado: " + isbn);
        Map<String, InfoDevolucion> map = prestamos.get(isbn);
        if (map == null || !map.containsKey(usuario))
            return new Transaccion(false, "No existe prestamo para ese usuario/ISBN");

        // Renovacion de un libro
        synchronized (libro) { // Manejo de hilos, blockeo para limitar las operaciones de Libros a un solo
                               // hilo
            InfoDevolucion info = map.get(usuario);
            if (info.contDevol >= RENOVACION_MAX) {
                return new Transaccion(false, "Limite de renovaciones alcanzado para ISBN: " + isbn);
            }
            // Renovacion simple: extender la fecha limite a por DIAS_PRESTAMO
            info.fechaLim = info.fechaLim.plusDays(DIAS_PRESTAMO);
            info.contDevol++;
            String mensaje = String.format(
                    "Renovacion exitosa. ISBN: %s, usuario: %s, nuevo vencimiento: %s, renovaciones: %d", isbn, usuario,
                    info.fechaLim, info.contDevol);
            return new Transaccion(true, mensaje, info.fechaLim);
        }
    }

    @Override
    public Transaccion devolucion(String isbn, String usuario) throws RemoteException {
        Libro libro = inventario.get(isbn);
        if (libro == null)
            return new Transaccion(false, "ISBN no encontrado: " + isbn);
        Map<String, InfoDevolucion> map = prestamos.get(isbn);
        if (map == null || !map.containsKey(usuario))
            return new Transaccion(false, "No existe prestamo para ese usuario/ISBN");

        // Devolucion de un libro prestado
        synchronized (libro) { // Manejo de hilos, blockeo para limitar las operaciones de Libros a un solo
                               // hilo
            map.remove(usuario);
            libro.devolver();
            String mensaje = String.format("Devolucion exitosa. ISBN: %s, usuario: %s. Copias disponibles: %d", isbn,
                    usuario, libro.getCopiasDisp());
            return new Transaccion(true, mensaje, null);
        }
    }

    @Override
    public Libro consulta(String isbn) throws RemoteException {
        Libro libro = inventario.get(isbn);
        return libro;
    }

    @Override
    public List<Libro> listaInventario() throws RemoteException {
        return new ArrayList<>(inventario.values());
    }
}