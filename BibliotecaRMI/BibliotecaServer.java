/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 22 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

// Servidor para la biblioteca
public class BibliotecaServer {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java BibliotecaServer <archivo-inventario.txt>");
            System.exit(1);
        }
        String invPath = args[0];
        try {
            // Levantar RMI Registry embebido en el puerto 1099
            try {
                LocateRegistry.createRegistry(1099);
                System.out.println("RMI Registry levantado en puerto 1099");
            } catch (RemoteException e) {
                System.out.println("RMI Registry ya estaba corriendo");
            }

            // Crear instancia del sistema de administracion de la biblioteca
            BibliotecaImpl impl = new BibliotecaImpl("MiBiblioteca");
            impl.cargarInventario(invPath);                 // Cargar inventario de libros
            System.out.println("Servidor de biblioteca listo.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}