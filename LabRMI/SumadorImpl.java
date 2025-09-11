/**********************
#      Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 4 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

// Implementación de la interfaz Sumador
public class SumadorImpl extends UnicastRemoteObject implements Sumador{
    public SumadorImpl(String name) throws RemoteException {
        super();
        try {
            System.out.println("Rebind objeto " + name);
            // Publicar el objeto con el nombre especificado
            Naming.rebind(name, this);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public int sumar (int a, int b) throws RemoteException
    { return a + b; }
    public int restar (int a, int b) throws RemoteException
    { return a - b; }
}