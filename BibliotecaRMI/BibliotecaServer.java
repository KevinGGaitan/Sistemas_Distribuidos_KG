/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 22 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class BibliotecaServer {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java BibliotecaServer <ip-servidor> <archivo-inventario.txt>");
            System.exit(1);
        }

        String ip = args[0];
        String invPath = args[1];

        try {
            // Levantar RMI Registry en puerto 1099
            try {
                LocateRegistry.createRegistry(1099);
                System.out.println("RMI Registry levantado en puerto 1099");
            } catch (RemoteException e) {
                System.out.println("RMI Registry ya estaba corriendo");
            }

            // Crear objeto remoto
            BibliotecaImpl impl = new BibliotecaImpl();
            impl.cargarInventario(invPath);

            // Publicar objeto en el Registry con la IP pasada por parámetro
            String url = "rmi://" + ip + ":1099/MiBiblioteca";
            Naming.rebind(url, impl);

            System.out.println("Servidor de biblioteca listo en " + url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
