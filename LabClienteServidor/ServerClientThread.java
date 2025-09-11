/*#**************************************************************
#         		Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 28 de Agosto 2025
#     Materia: Sistemas Distribuidos
#****************************************************************/

import java.net.*;
import java.io.*;
class ServerClientThread extends Thread {
  Socket serverClient;                          // Socket del cliente
  int clientNo;                                 // Número de identificación del cliente
  int squre;                                    // Almacena el valor al cuadrado
  
  // Constructor para inicializar el socket del cliente y su ID
  ServerClientThread(Socket inSocket,int counter){
    serverClient = inSocket;
    clientNo=counter;
  }
  
  public void run(){
    try{
      // Crear streams de entrada/salida para la comunicación
      DataInputStream inStream = new DataInputStream(serverClient.getInputStream());
      DataOutputStream outStream = new DataOutputStream(serverClient.getOutputStream());
      String clientMessage = "", serverMessage = "";
      
      while(!clientMessage.equals("bye")){          // Recibir mensajes hasta que el cliente envíe "bye"
        clientMessage = inStream.readUTF();         // Leer mensaje del cliente
        System.out.println("From Client-" +clientNo+ ": Number is :"+clientMessage);      // Mostrar info de mensajer recibidos
        squre = Integer.parseInt(clientMessage) * Integer.parseInt(clientMessage);        // Calcular el cuadrado del número recibido
        
        
        serverMessage="From Server to Client-" + clientNo + " Square of " + clientMessage + " is " +squre;  // Preparar mensaje de respuesta
        outStream.writeUTF(serverMessage);                                                 // Enviar respuesta al cliente
        outStream.flush();
      }
      
      // Liberar recursos
      inStream.close();
      outStream.close();
      serverClient.close();
    } catch(Exception ex){
      System.out.println(ex);                        // Manejo de errores
    } finally{
      System.out.println("Client -" + clientNo + " exit!! ");         // Mensaje de cierre del servidor
    }
  }
}
