/**********************
#      Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 4 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/
import java.rmi.*;
import java.rmi.server.*;

// Servidor para el sumador
public class SumadorServer {
    public static void main (String args[]) {
        // Establecer el administrador de seguridad
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
        try{
            // Crear instancia del sumador
            SumadorImpl misuma = new SumadorImpl("MiSumador");
        } catch(Exception excr) {
            System.out.println("Excepcion: "+excr);
        }
    }
}
