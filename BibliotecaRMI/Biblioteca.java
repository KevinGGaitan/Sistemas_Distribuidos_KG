/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 22 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Biblioteca extends Remote {
    Transaccion prestamo(String isbn, String usuario) throws RemoteException;
    Transaccion prestamoNombre(String isbn, String usuario) throws RemoteException;
    Transaccion renovacion(String isbn, String usuario) throws RemoteException;
    Transaccion devolucion(String isbn, String usuario) throws RemoteException;
    Libro consulta(String isbn) throws RemoteException;
    List<Libro> listaInventario() throws RemoteException;
}
