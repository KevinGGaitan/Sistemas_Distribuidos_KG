/**************************************************************
#                         Pontificia Universidad Javeriana
#     Autor: Juan Bello, Kevin Garay, Arley Bernal
#     Fecha: 30 de Septiembre 2025
#     Materia: Sistemas Distribuidos
**************************************************************/
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

/*
 * Gestor de Almacenamiento (GA)
 * - Patrón REP (Request-Reply) para comunicación sincrónica
 * - Almacenamiento en archivo JSON
 * - Soporte para réplica secundaria (alta disponibilidad)
 * 
 * Operaciones:
 * - GET_LIBRO: Consulta información de libro por ISBN
 * - UPDATE_LIBRO: Actualiza estado de libro en inventario
 * 
 * Replica:
 * - Replicación síncrona opcional a GA secundario
 * - Tolerancia a fallos en réplica
 */
public class GestorAlmacenamiento {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Libro> inventario = new HashMap<>();
    private static String archInv; // Ruta del archivo de inventario

    // Configuración de réplica
    private static boolean tieneReplica = false;
    private static String replicaHost;
    private static int replicaPort;

    //Carga el inventario desde archivo JSON
    private static Map<String, Libro> cargarInventario(String file) {
        try {
            if (Files.exists(Paths.get(file))) {
                String content = Files.readString(Paths.get(file));
                // Deserializar array JSON a array de objetos Libro
                Libro[] libros = gson.fromJson(content, Libro[].class);
                Map<String, Libro> map = new HashMap<>();
                for (Libro l : libros) map.put(l.getIsbn(), l);
                System.out.println("Inventario cargado (" + map.size() + " libros)");
                return map;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashMap<>(); // Inventario vacío si hay error
    }

    //Persiste el inventario actual en archivo JSON
    private static void guardarCambio() {
        String json = gson.toJson(inventario.values()); // Serializar valores del mapa
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

        // Configurar réplica si se proporcionan parámetros
        if (args.length >= 4) {
            tieneReplica = true;
            replicaHost = args[2];
            replicaPort = Integer.parseInt(args[3]);
        }

        // Cargar inventario inicial
        inventario = cargarInventario(archInv);

        try (ZContext ctx = new ZContext()) {
            // SOCKET REP Para recibir peticiones de actores
            ZMQ.Socket rep = ctx.createSocket(SocketType.REP);
            rep.bind("tcp://*:" + port);
            System.out.println("GA escuchando en puerto " + port);

            // Configurar socket para réplica (si aplica)
            ZMQ.Socket replicaReq = null;
            if (tieneReplica) {
                replicaReq = ctx.createSocket(SocketType.REQ);
                replicaReq.setReceiveTimeOut(2000); // Timeout de 2 segundos
                try {
                    replicaReq.connect("tcp://" + replicaHost + ":" + replicaPort);
                    System.out.println("GA conectado a réplica en " + replicaHost + ":" + replicaPort);
                } catch (Exception e) {
                    System.out.println("No se pudo conectar con la replica inicialmente");
                    tieneReplica = false; // Continuar sin réplica
                }
            }

            // Bucle principal de atención de peticiones
            while (!Thread.currentThread().isInterrupted()) {
                String msg = rep.recvStr();
                if (msg == null) break;

                System.out.println("GA recibió: " + msg);
                JsonObject req = JsonParser.parseString(msg).getAsJsonObject();
                String tipo = req.get("type").getAsString();

                JsonObject resp = new JsonObject();

                // Procesar según tipo de operación
                switch (tipo) {
                    case "GET_LIBRO": {
                        String isbn = req.get("isbn").getAsString();
                        Libro libro = inventario.get(isbn);
                        if (libro != null) {
                            resp.addProperty("estatus", "OK");
                            resp.add("libro", gson.toJsonTree(libro)); // Serializar objeto Libro
                        } else {
                            resp.addProperty("estatus", "ERROR");
                            resp.addProperty("mensaje", "Libro no encontrado");
                        }
                        break;
                    }

                    case "UPDATE_LIBRO": {
                        JsonObject libroJson = req.getAsJsonObject("libro");
                        Libro libro = gson.fromJson(libroJson, Libro.class); // Deserializar
                        inventario.put(libro.getIsbn(), libro);
                        guardarCambio(); // Persistir en disco
                        resp.addProperty("estatus", "OK");
                        resp.addProperty("mensaje", "Libro actualizado localmente");

                        // Propagación síncrona a secundario
                        if (tieneReplica && replicaReq != null) {
                            try {
                                System.out.println("Replicando actualizacion al GA secundario...");
                                replicaReq.send(msg); // Reenviar mismo mensaje
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

                rep.send(gson.toJson(resp)); // Enviar respuesta
            }
        }
    }
}