/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 12 de Noviembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/

package com.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Coordinador {
    private final Map<String, String> participants;
    private final ZContext context;
    private final Scanner scanner;

    public Coordinador() {
        this.participants = new ConcurrentHashMap<>();
        this.context = new ZContext();
        this.scanner = new Scanner(System.in);
    }

    public void start(int port) {
        try (ZMQ.Socket clientSocket = context.createSocket(ZMQ.REP);
             ZMQ.Socket participantSocket = context.createSocket(ZMQ.REQ)) {
             
            clientSocket.bind("tcp://*:" + port);
            
            // Definicion de participantes estatica
            participants.put("nodo1", "tcp://localhost:5556");
            participants.put("nodo2", "tcp://localhost:5557");

            System.out.println("Coordinador inicializado en puerto " + port);
            System.out.println("Participantes encontrados: " + participants.keySet());
            System.out.println("Escribe 'inicio' para empezar una transaccion");
            System.out.println("----------------------------------------");

            while (!Thread.currentThread().isInterrupted()) {
                // Esperar comando de inicio
                System.out.print("\nEscribe 'inicio' para empezar una transaccion: ");
                String command = scanner.nextLine().trim();
                
                if ("inicio".equalsIgnoreCase(command)) {
                    transaccion();
                } else if ("salir".equalsIgnoreCase(command)) {
                    break;
                } else {
                    System.out.println("Comando desconocido. Use 'inicio' or 'salir'");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.close();
        }
    }

    private void transaccion() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Inicializando una nueva transaccion");
        System.out.println("=".repeat(50));
        
        // Fase 1: preparacion y votacion
        System.out.println("\n=== FASE 1: Fase de votacion ===");
        boolean allPrepared = fase1();
        
        // Fase 2: decision final
        System.out.println("\n=== FASE 2: Fase de decision ===");
        if (allPrepared) {
            System.out.println("Todos los participantes votaron COMMIT - realizando GLOBAL COMMIT");
            fase2("GLOBAL_COMMIT");
        } else {
            System.out.println("Uno o mas participantes voto ABORT - realizando GLOBAL ABORT");
            fase2("GLOBAL_ABORT");
        }
    }

    private boolean fase1() {
        Map<String, ZMQ.Socket> participanteSockets = new HashMap<>();
        List<Boolean> votes = new ArrayList<>();

        try {
            // Crear y conectar sockets con los participantes
            for (Map.Entry<String, String> entry : participants.entrySet()) {
                ZMQ.Socket socket = context.createSocket(ZMQ.REQ);
                socket.setReceiveTimeOut(10000); // timeout en 10 segundos
                socket.connect(entry.getValue());
                participanteSockets.put(entry.getKey(), socket);
            }

            // Envia PREPARE  a todos los participantes
            System.out.println("Enviando mensaje PREPARE a todos los participantes...");
            for (Map.Entry<String, ZMQ.Socket> entry : participanteSockets.entrySet()) {
                String idparticipante = entry.getKey();
                ZMQ.Socket socket = entry.getValue();
                socket.send("PREPARE");
                System.out.println("PREPARE enviado a " + idparticipante);
            }

            // Recolecta los votos de todos lo participantes
            System.out.println("\nEsperando el voto de los participantes...");
            for (Map.Entry<String, ZMQ.Socket> entry : participanteSockets.entrySet()) {
                String idparticipante = entry.getKey();
                ZMQ.Socket socket = entry.getValue();
                
                String vote = socket.recvStr();
                if (vote != null) {
                    System.out.println("Recibido de " + idparticipante + ": " + vote);
                    votes.add("VOTE_COMMIT".equals(vote));
                    
                    if ("VOTE_ABORT".equals(vote)) {
                        System.out.println(idparticipante + " voto ABORT!!!!!");
                    }
                } else {
                    System.out.println("Timeout de espera para el voto de " + idparticipante);
                    votes.add(false);
                }
            }

            // Verificar que todos votaron COMMIT
            for (boolean vote : votes) {
                if (!vote) {
                    return false;
                }
            }
            return true;

        } catch (Exception e) {
            System.out.println("Error durante la faase 1: " + e.getMessage());
            return false;
        } finally {
            // liverar sockets
            for (ZMQ.Socket socket : participanteSockets.values()) {
                socket.close();
            }
        }
    }

    private void fase2(String decision) {
        Map<String, ZMQ.Socket> participanteSockets = new HashMap<>();

        try {
            // Crea y conecta sockets a todos los particopantes
            for (Map.Entry<String, String> entry : participants.entrySet()) {
                ZMQ.Socket socket = context.createSocket(ZMQ.REQ);
                socket.setReceiveTimeOut(10000); // timeout de 10 segundos
                socket.connect(entry.getValue());
                participanteSockets.put(entry.getKey(), socket);
            }

            // Envia la decicion final a todos los participantes
            System.out.println("Enviando " + decision + " a todos los participantes...");
            for (Map.Entry<String, ZMQ.Socket> entry : participanteSockets.entrySet()) {
                String idparticipante = entry.getKey();
                ZMQ.Socket socket = entry.getValue();
                socket.send(decision);
                System.out.println(decision + " enviada a " + idparticipante);
            }

            // Esperar ACKs de todos los participantes
            System.out.println("\nEsperandp ACKs de los participantes...");
            for (Map.Entry<String, ZMQ.Socket> entry : participanteSockets.entrySet()) {
                String idparticipante = entry.getKey();
                ZMQ.Socket socket = entry.getValue();
                
                String ack = socket.recvStr();
                if (ack != null) {
                    System.out.println("ACK recibido de " + idparticipante + ": " + ack);
                } else {
                    System.out.println("Timeout para recibir ACK de " + idparticipante);
                }
            }

            System.out.println("\nTransaccion finalizada: " + decision);

        } catch (Exception e) {
            System.out.println("Error en fase 2: " + e.getMessage());
        } finally {
            // Libreacion de sockets
            for (ZMQ.Socket socket : participanteSockets.values()) {
                socket.close();
            }
        }
    }

    public static void main(String[] args) {
        Coordinador coordinador = new Coordinador();
        coordinador.start(5555);
    }
}