/*#**************************************************************
#         		Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 28 de Agosto 2025
#     Materia: Sistemas Distribuidos
#****************************************************************/

import java.net.*;
import java.io.*;

public class TCPClient {
  public static void main(String[] args) throws Exception {
    try{
      // Conectar al servidor en la ip del servidor con puerto 8888
      Socket socket=new Socket("127.0.0.1", 8888);
      DataInputStream inStream=new DataInputStream(socket.getInputStream());        // Crear streams de entrada
      DataOutputStream outStream=new DataOutputStream(socket.getOutputStream());    // Crear streams de salida
      BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
      String clientMessage="",serverMessage="";

      while(!clientMessage.equals("bye")){                                          // Continuar hasta que el usuario escriba "bye"
        System.out.println("Enter number :");
        clientMessage=br.readLine();                                                // Leer entrada del usuario
        outStream.writeUTF(clientMessage);                                          // Enviar al servidor
        outStream.flush();
        serverMessage=inStream.readUTF();                                           // Leer respuesta del servidor
        System.out.println(serverMessage);
      }
      
      // Liberar recursos
      outStream.close();
      inStream.close();
      socket.close();
    } catch(Exception e){                                                             //manejo de errores
      System.out.println(e);
    }
  }
}
