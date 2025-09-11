/**********************
#      Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 4 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/
// Interfaz remota para operaciones matemáticas
public interface Sumador extends java.rmi.Remote {
    public int sumar(int a, int b)
    throws java.rmi.RemoteException;
    public int restar(int a, int b)
    throws java.rmi.RemoteException;
}
