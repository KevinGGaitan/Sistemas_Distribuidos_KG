/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
#**************************************************************/
package sd.library;

import com.google.gson.*;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import sd.library.model.Libro;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Gestor de Almacenamiento (GA)
 * - Recibe resultados de actores (PRESTAMO_RESULT, DEVOLUCION_RESULT, RENOVACION_RESULT)
 * - Actualiza la base de datos local (archivo JSON)
 * - Replica los cambios al GA secundario si está configurado
 */

public class GestorAlmacenamiento {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Libro> inventario = new HashMap<>();
    private static String archInv;

    private static boolean tieneReplica = false;
    private static String replicaHost;
    private static int replicaPort;


    private static Map<String, Libro> cargarInventario(String file) {
        try {
            if (Files.exists(Paths.get(file))) {
                String content = Files.readString(Paths.get(file));
                Libro[] libros = gson.fromJson(content, Libro[].class);
                Map<String, Libro> map = new HashMap<>();
                for (Libro l : libros) map.put(l.getIsbn(), l);
                System.out.println("Inventario cargado (" + map.size() + " libros)");
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    private static void guardarCambio() {
        String json = gson.toJson(inventario.values());
        try {
            Files.writeString(Paths.get(archInv), json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Inventario actualizado");
    }
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java GestorAlmacenamiento <puerto> <archivoJSON> [hostSecundario] [puertoSecundario]");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        archInv = args[1];

        if (args.length >= 4) {
            tieneReplica = true;
            replicaHost = args[2];
            replicaPort = Integer.parseInt(args[3]);
        }

        inventario = cargarInventario(archInv);

        try (ZContext ctx = new ZContext()) {
            ZMQ.Socket rep = ctx.createSocket(SocketType.REP);
            rep.bind("tcp://*:" + port);
            System.out.println("GA escuchando en puerto " + port);

            ZMQ.Socket replicaReq = null;
            if (tieneReplica) {
                replicaReq = ctx.createSocket(SocketType.REQ);
                replicaReq.setReceiveTimeOut(2000);
                try {
                    replicaReq.connect("tcp://" + replicaHost + ":" + replicaPort);
                    System.out.println("GA conectado a réplica en " + replicaHost + ":" + replicaPort);
                } catch (Exception e) {
                    System.out.println("No se pudo conectar con la replica inicialmente");
                    tieneReplica = false;
                }
            }

            while (!Thread.currentThread().isInterrupted()) {
                String msg = rep.recvStr();
                if (msg == null) break;

                System.out.println("GA recibió: " + msg);
                JsonObject req = JsonParser.parseString(msg).getAsJsonObject();
                String tipo = req.get("type").getAsString();

                JsonObject resp = new JsonObject();

                switch (tipo) {
                    case "GET_LIBRO": {
                        String isbn = req.get("isbn").getAsString();
                        Libro libro = inventario.get(isbn);
                        if (libro != null) {
                            resp.addProperty("estatus", "OK");
                            resp.add("libro", gson.toJsonTree(libro));
                        } else {
                            resp.addProperty("estatus", "ERROR");
                            resp.addProperty("mensaje", "Libro no encontrado");
                        }
                        break;
                    }

                    case "UPDATE_LIBRO": {
                        JsonObject libroJson = req.getAsJsonObject("libro");
                        Libro libro = gson.fromJson(libroJson, Libro.class);
                        inventario.put(libro.getIsbn(), libro);
                        guardarCambio
                
                ();
                        resp.addProperty("estatus", "OK");
                        resp.addProperty("mensaje", "Libro actualizado localmente");


                        // replicar si aplica
                        if (tieneReplica && replicaReq != null) {
                            try {
                                System.out.println("Replicando actualizacion al GA secundario...");
                                replicaReq.send(msg);
                                String replicaResp = replicaReq.recvStr();
                                if (replicaResp != null) {
                                    JsonObject r = JsonParser.parseString(replicaResp).getAsJsonObject();
                                    if (r.has("estatus") && r.get("estatus").getAsString().equals("OK")) {
                                        System.out.println("Replica actualizada correctamente");
                                    } else {
                                        System.out.println("Replica respondió con error");
                                    }
                                } else {
                                    System.out.println("Replica no respondió");
                                }
                            } catch (Exception e) {
                                System.out.println("Error al replicar: " + e.getMessage());
                            }
                        }

                        break;
                    }

                    default:
                        resp.addProperty("estatus", "ERROR");
                        resp.addProperty("mensaje", "Tipo desconocido: " + tipo);
                }

                rep.send(gson.toJson(resp));
            }
        }
    }
}
