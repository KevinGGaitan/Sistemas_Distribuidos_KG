/*#**************************************************************
#         		Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 28 de Agosto 2025
#     Materia: Sistemas Distribuidos
#****************************************************************/

import java.net.*;
import java.io.*;

public class MultithreadedSocketServer {
  public static void main(String[] args) throws Exception {
    try{
      // Crear socket del servidor que escucha en el puerto 8888
      ServerSocket server=new ServerSocket(8888);
      int counter=0;
      System.out.println("Server Started ....");
      
      // Bucle infinito para aceptar conexiones de clientes continuamente
      while(true){
        counter++;                      // Contador de clientes
        // Aceptar conexión entrante del cliente
        Socket serverClient=server.accept();
        System.out.println(" >> " + "Client No:" + counter + " started!");
        
        // Crear nuevo hilo para manejar la comunicación con cada cliente
        ServerClientThread sct = new ServerClientThread(serverClient,counter);
        sct.start();                    // Iniciar el hilo
      }
    } catch(Exception e){
      System.out.println(e);
    }
  }
}
