package com.mycompany.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatServer {
    private int port;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Set<ClientHandler> clients;
    private volatile boolean running;

    // Constructor por defecto (puerto 9000)
    public ChatServer() {
        this(9000);
    }

    // Constructor con puerto configurable
    public ChatServer(int port) {
        this.port = port;
        this.clients = ConcurrentHashMap.newKeySet();
        this.threadPool = Executors.newCachedThreadPool();
        this.running = true;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("===========================================");
            System.out.println("    Servidor de Chat iniciado en puerto " + port);
            System.out.println("===========================================");
            System.out.println("Esperando conexiones de clientes...\n");

            // Verificar conexión a DB
            if (Database.testConnection()) {
                System.out.println("Conexion a base de datos exitosa\n");
            } else {
                System.err.println("Error: No se pudo conectar a la base de datos");
                System.err.println("  Asegurate de que MySQL este corriendo y ejecuta schema.sql\n");
                return;
            }

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nueva conexion desde: " + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    threadPool.submit(handler);

                } catch (SocketException e) {
                    if (running) {
                        System.err.println("Error en socket: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    // Envía un mensaje a todos los clientes excepto al emisor
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender && client.isAuthenticated()) {
                client.sendMessage(message);
            }
        }
    }

    // Envía un archivo a todos los clientes excepto al emisor
    public void broadcastFile(String fileName, byte[] fileData, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender && client.isAuthenticated()) {
                client.sendFile(fileName, fileData);
            }
        }
    }

    // Agrega un cliente a la sala
    public void addClient(ClientHandler client) {
        clients.add(client);
        System.out.println("Usuario autenticado: " + client.getUsername()
                + " (Total conectados: " + clients.size() + ")");
    }

    // Remueve un cliente de la sala
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        if (client.getUsername() != null) {
            System.out.println("Usuario desconectado: " + client.getUsername()
                    + " (Total conectados: " + clients.size() + ")");
            broadcast("SYSTEM|" + client.getUsername() + " se ha desconectado", client);
        }
    }

    // Detiene el servidor
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // Cerrar todas las conexiones de clientes
            for (ClientHandler client : clients) {
                client.close();
            }

            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }

            System.out.println("\nServidor detenido correctamente.");
        } catch (IOException | InterruptedException e) {
            System.err.println("Error al detener el servidor: " + e.getMessage());
        }
    }

    // main de prueba (puedes dejarlo o ignorarlo si usas Main.java)
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.start();
    }
}
