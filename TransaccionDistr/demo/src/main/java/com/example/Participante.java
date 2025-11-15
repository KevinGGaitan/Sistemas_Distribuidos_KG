/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Noviembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

package com.example;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import java.util.Scanner;

public class Participante {
    private final String id;
    private final String coordinadorAddress;
    private final Scanner scanner;

    public Participante(String id, String coordinadorAddress) {
        this.id = id;
        this.coordinadorAddress = coordinadorAddress;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try (ZContext context = new ZContext();
             ZMQ.Socket socket = context.createSocket(ZMQ.REP)) {
            
            // Asignar direccion unioca del participante
            String participanteAddress = getDireccionParticipante(id);
            socket.bind(participanteAddress);
            
            System.out.println("Participante " + id + " inicializado");
            System.out.println("Escuchando en: " + participanteAddress);
            System.out.println("Esperando mensaje del coordinador...");
            System.out.println("----------------------------------------");

            while (!Thread.currentThread().isInterrupted()) {
                // Esperar mensaje del coordinador
                String mensaje = socket.recvStr();
                System.out.println("\n=== Mensaje recibido: " + mensaje + " ===");

                if (mensaje.startsWith("PREPARE")) {
                    // Fase 1: votacion
                    System.out.print(id + " vota COMMIT? (si/no): ");
                    String decision = scanner.nextLine().trim().toLowerCase();

                    String respuesta;
                    if ("si".equals(decision)) {
                        respuesta = "VOTE_COMMIT";
                        System.out.println("Votando COMMIT");
                    } else {
                        respuesta = "VOTE_ABORT";
                        System.out.println("Votando ABORT");
                    }
                    
                    socket.send(respuesta);
                    System.out.println("Mandando voto al coordinador: " + respuesta);

                } else if (mensaje.startsWith("GLOBAL_COMMIT")) {
                    // Fase 2: resultado COMMIT
                    System.out.println("GLOBAL_COMMIT recibido - Transaction realizada");
                    socket.send("ACK");
                    System.out.println("ACK anviado al coordinador");

                } else if (mensaje.startsWith("GLOBAL_ABORT")) {
                    // Fase 2: resultado ABORT
                    System.out.println("GLOBAL_ABORT recibido - Transaction cancelada");
                    socket.send("ACK");
                    System.out.println("ACK anviado al coordinador");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getDireccionParticipante(String participantId) {
        switch (participantId) {
            case "nodo1": return "tcp://*:5556";
            case "nodo2": return "tcp://*:5557";
            case "nodo3": return "tcp://*:5558";
            default: return "tcp://*:5559";
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: Participante <id> <coordinator_address>");
            System.out.println("Ejemplo: Participante nodo1 tcp://localhost:5555");
            return;
        }

        String id = args[0];
        String coordinadorAddress = args[1];

        Participante participante = new Participante(id, coordinadorAddress);
        participante.start();
    }
}