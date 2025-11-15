package com.mycompany.chat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.mycompany.chat.util.Constants;
import com.mycompany.chat.observer.ServerEventSubject;
import com.mycompany.chat.observer.ServerEventData;
import com.mycompany.chat.observer.ServerEvent;
import com.mycompany.chat.observer.ServerEventLogger;

public class ChatServer {
    private int port;
    private ServerSocket serverSocket;
    private ServerSocket videoServer;
    private ExecutorService threadPool;
    private Set<ClientHandler> clients;
    private volatile boolean running;
    private final ServerEventSubject eventSubject; // Observer Pattern: Subject para notificar eventos

    // Constructor por defecto (puerto 9000)
    public ChatServer() {
        this(9000);
    }

    // Constructor con puerto configurable
    public ChatServer(int port) {
        this.port = port;
        this.clients = ConcurrentHashMap.newKeySet();
        this.threadPool = Executors.newFixedThreadPool(Constants.SERVER_THREAD_POOL_SIZE);
        this.running = true;
        // Inicializar Observer Pattern: Subject y logger por defecto
        this.eventSubject = new ServerEventSubject();
        this.eventSubject.addObserver(new ServerEventLogger(false)); // Logger silencioso por defecto
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            videoServer = new ServerSocket(port + Constants.DEFAULT_VIDEO_PORT_OFFSET);
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
                    Socket clientVideo = videoServer.accept();
                    System.out.println("Nueva conexion desde: " + clientSocket.getInetAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, this, clientVideo);
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
    // Envía un mensaje a todos los clientes excepto al emisor (para mensajes del sistema)
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender && client.isAuthenticated()) {
                client.sendMessage(message);
            }
        }
    }
    
    // Envía un mensaje privado a un destinatario específico
    public boolean sendPrivateMessage(String message, String recipient, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client.isAuthenticated() && client.getUsername().equals(recipient)) {
                client.sendMessage(message);
                // Observer Pattern: Notificar evento de mensaje enviado
                if (sender.getUsername() != null) {
                    eventSubject.notifyObservers(new ServerEventData(
                        ServerEvent.PRIVATE_MESSAGE_SENT, sender.getUsername(), recipient, message, null));
                }
                return true; // Destinatario encontrado
            }
        }
        return false; // Destinatario no encontrado
    }
    
    // Obtiene la lista de usuarios conectados (excepto el solicitante)
    public List<String> getConnectedUsers(ClientHandler requester) {
        List<String> users = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (client.isAuthenticated() && client != requester) {
                users.add(client.getUsername());
            }
        }
        return users;
    }
    
    // Obtiene un ClientHandler por username
    public ClientHandler getClientByUsername(String username) {
        for (ClientHandler client : clients) {
            if (client.isAuthenticated() && client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }
    
    // Envía video privado a un destinatario específico
    public boolean sendPrivateVideo(byte[] frame, String recipient, ClientHandler sender) {
        ClientHandler target = getClientByUsername(recipient);
        if (target != null && target.isAuthenticated() && target.getVideoSocket() != null) {
            try {
                DataOutputStream out = new DataOutputStream(target.getVideoSocket().getOutputStream());
                byte[] nameBytes = sender.getUsername().getBytes();
                out.writeInt(nameBytes.length);
                out.write(nameBytes);

                // Enviar longitud del frame y el frame mismo
                out.writeInt(frame.length);
                out.write(frame);
                out.flush();
                return true;
            } catch (IOException e) {
                System.err.println("Error enviando frame a " + recipient + ": " + e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    // Envía un archivo privado a un destinatario específico
    public boolean sendPrivateFile(String fileName, byte[] fileData, String recipient, ClientHandler sender) {
        ClientHandler target = getClientByUsername(recipient);
        if (target != null && target.isAuthenticated()) {
            target.sendFile(fileName, fileData);
            // Observer Pattern: Notificar evento de archivo enviado
            if (sender.getUsername() != null) {
                eventSubject.notifyObservers(new ServerEventData(
                    ServerEvent.FILE_SENT, sender.getUsername(), recipient, fileName, fileData));
            }
            return true;
        }
        return false;
    }
    
    /**
     * Obtiene el Subject de eventos para permitir registrar observadores externos.
     * Observer Pattern: Método para acceder al Subject.
     * 
     * @return ServerEventSubject para registrar observadores
     */
    public ServerEventSubject getEventSubject() {
        return eventSubject;
    }

    // Agrega un cliente a la sala
    public void addClient(ClientHandler client) {
        clients.add(client);
        System.out.println("Usuario autenticado: " + client.getUsername()
                + " (Total conectados: " + clients.size() + ")");
        // Observer Pattern: Notificar evento de conexión
        if (client.getUsername() != null) {
            eventSubject.notifyObservers(new ServerEventData(
                ServerEvent.USER_CONNECTED, client.getUsername()));
        }
    }

    // Remueve un cliente de la sala
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        if (client.getUsername() != null) {
            System.out.println("Usuario desconectado: " + client.getUsername()
                    + " (Total conectados: " + clients.size() + ")");
            broadcast(Constants.RESP_SYSTEM + Constants.PROTOCOL_SEPARATOR + 
                     client.getUsername() + " se ha desconectado", client);
            // Observer Pattern: Notificar evento de desconexión
            eventSubject.notifyObservers(new ServerEventData(
                ServerEvent.USER_DISCONNECTED, client.getUsername()));
        }
    }

    // Detiene el servidor
    public void shutdown() {
        running = false;
        try {
            // Cerrar todas las conexiones de clientes primero
            for (ClientHandler client : clients) {
                try {
                    client.close();
                } catch (Exception e) {
                    System.err.println("Error cerrando cliente: " + e.getMessage());
                }
            }

            // Cerrar sockets del servidor
            if (videoServer != null && !videoServer.isClosed()) {
                try {
                    videoServer.close();
                } catch (IOException e) {
                    System.err.println("Error cerrando videoServer: " + e.getMessage());
                }
            }
            
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    System.err.println("Error cerrando serverSocket: " + e.getMessage());
                }
            }

            // Cerrar pool de threads
            if (threadPool != null) {
                threadPool.shutdown();
                try {
                    if (!threadPool.awaitTermination(Constants.THREAD_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        threadPool.shutdownNow();
                        if (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                            System.err.println("Algunos threads no terminaron correctamente");
                        }
                    }
                } catch (InterruptedException e) {
                    threadPool.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("\nServidor detenido correctamente.");
        } catch (Exception e) {
            System.err.println("Error al detener el servidor: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.start();
    }
}
