/**********************
#      Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 4 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**********************/
import java.rmi.*;

// Cliente para el servicio Sumador
public class SumadorClient {
    public static void main(String args[]) {
        int res = 0;
        // Numeros a sumar 
        int a = 5;
        int b = 2;
        try {
            System.out.println("Buscando Objeto ");
            // Buscar el objeto remoto, la dirección del servidor se pasa como argumento
            Sumador misuma = (Sumador)Naming.lookup(
            "rmi://" + args[0] + "/" +"MiSumador");
            // Invocar método remoto
            res = misuma.sumar(a, b);
            System.out.println(a + " + " + b + " = " + res);
        } catch(Exception e) {
            System.err.println(" System exception");
        }
        System.exit(0);
    }
}
