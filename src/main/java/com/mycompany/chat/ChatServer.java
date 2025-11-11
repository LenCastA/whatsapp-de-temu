package com.mycompany.chat;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 9000;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Set<ClientHandler> clients;
    private volatile boolean running;

    public ChatServer() {
        this.clients = ConcurrentHashMap.newKeySet();
        this.threadPool = Executors.newCachedThreadPool();
        this.running = true;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("===========================================");
            System.out.println("    Servidor de Chat iniciado en puerto " + PORT);
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

    /**
     * Envía un mensaje a todos los clientes excepto al emisor
     */
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender && client.isAuthenticated()) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Envía un archivo a todos los clientes excepto al emisor
     */
    public void broadcastFile(String fileName, byte[] fileData, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender && client.isAuthenticated()) {
                client.sendFile(fileName, fileData);
            }
        }
    }

    /**
     * Agrega un cliente a la sala
     */
    public void addClient(ClientHandler client) {
        clients.add(client);
        System.out.println("Usuario autenticado: " + client.getUsername()
                + " (Total conectados: " + clients.size() + ")");
    }

    /**
     * Remueve un cliente de la sala
     */
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        if (client.getUsername() != null) {
            System.out.println("Usuario desconectado: " + client.getUsername()
                    + " (Total conectados: " + clients.size() + ")");
            broadcast("SYSTEM|" + client.getUsername() + " se ha desconectado", client);
        }
    }

    /**
     * Detiene el servidor
     */
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

    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        // Agregar shutdown hook para cerrar limpiamente
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        server.start();
    }
}
