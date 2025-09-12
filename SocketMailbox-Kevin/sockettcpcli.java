/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 11 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

import java.net.*;               // Libreria de red (Socket, ServerSocket, DatagramSocket, InetAddress, ...)
import java.io.*;

// Definición de la clase principal del programa cliente.
public class sockettcpcli {
   public static void main(String argv[]) {
      if (argv.length == 0) {
         System.err.println("java sockettcpcli servidor");
         System.exit(1);
      }
      // Crear un BufferedReader para leer texto de la entrada del socket.
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));     // InputStreamReader decodifica bytes a caracteres usando la codificación por defecto.

      System.out.println("Prueba de sockets TCP (cliente)");
      Socket socket;
      InetAddress address;
      byte[] mensaje_bytes = new byte[256];
      String mensaje="";

      // Conexion con el servidor especificado, direccion ip por argumento y puerto 6001
      try {
         System.out.print("Capturando dirección de host... ");
         address=InetAddress.getByName(argv[0]);
         System.out.println("ok");

         System.out.print("Creando socket... ");
         socket = new Socket(address,6001);                       // Puerto del servidor TCP
         System.out.println("ok");

         DataOutputStream out =
            new DataOutputStream(socket.getOutputStream());

         System.out.println("Introduce mensajes a enviar:");

         do {
            mensaje = in.readLine();
            out.writeUTF(mensaje);
         } while (!mensaje.startsWith("fin"));
      }
      catch (Exception e) {
         System.err.println(e.getMessage());
         System.exit(1);
      }
   }
}

