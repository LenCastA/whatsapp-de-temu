package com.mycompany.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

import com.mycompany.chat.protocol.MessageHandler;
import com.mycompany.chat.protocol.MessageHandlerRegistry;
import com.mycompany.chat.protocol.MessageBuilder;
import com.mycompany.chat.service.DatabaseService;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private final Socket videoClient;
    private DataInputStream dataIn;
    private DataInputStream videoIn;
    private DataOutputStream dataOut;
    private String username;
    private boolean videoActive = false;
    private String videoRecipient = null; // Destinatario para video privado
    private boolean authenticated;
    private volatile boolean running;
    private final MessageHandlerRegistry handlerRegistry; // Registry para Strategy Pattern
    private final DatabaseService databaseService;
    
    public Socket getSocket(){return socket;}
    public String getUsername(){return username;}
    public Socket getVideoSocket(){return videoClient;}
    public boolean getVideoActive(){return videoActive;}
    // Métodos públicos para los handlers del Strategy Pattern
    public ChatServer getServer() { return server; }
    public boolean isAuthenticated() { return authenticated; }
    public void setUsername(String username) { this.username = username; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
    public void setRunning(boolean running) { this.running = running; }

    public byte[] readBytes(int length) throws IOException {
        byte[] data = new byte[length];
        dataIn.readFully(data);
        return data;
    }

    public void startVideoStream(String recipient) throws IOException {
        if (videoActive) {
            throw new IllegalStateException("Ya existe una videollamada activa");
        }
        if (videoClient == null) {
            throw new IOException("Socket de video no disponible");
        }
        videoIn = new DataInputStream(videoClient.getInputStream());
        videoActive = true;
        videoRecipient = recipient;
        ExecutorService executor = server.getThreadPool();
        if (executor != null) {
            executor.submit(this::receiveVideo);
        } else {
            new Thread(this::receiveVideo).start();
        }
    }

    public void stopVideoStream() {
        videoActive = false;
        videoRecipient = null;
        if (videoIn != null) {
            try {
                videoIn.close();
            } catch (IOException ignored) {}
        }
    }

    public void sendServerMessage(String message) {
        sendMessage(MessageBuilder.buildServerMessage(message));
    }

    public void sendError(String message) {
        sendMessage(MessageBuilder.buildError(message));
    }

    public void sendOk(String command, String message) {
        sendMessage(MessageBuilder.buildOk(command, message));
    }

    public ClientHandler(Socket socket, ChatServer server, Socket videoSocket, DatabaseService databaseService) {
        this.socket = socket;
        this.videoClient = videoSocket;
        this.server = server;
        this.authenticated = false;
        this.running = true;
        this.databaseService = databaseService;
        this.handlerRegistry = new MessageHandlerRegistry(databaseService);
    }

    @Override
    public void run() {
        try {
            // Inicializar streams
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());

            sendServerMessage("Bienvenido al servidor de chat. Por favor inicia sesión.");

            while (running) {
                String line = dataIn.readUTF();
                processMessage(line);
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("Error en ClientHandler (" + username + "): " + e.getMessage());
            }
        } finally {
            close();
        }
    }

    private void processMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        // Parsear comando (puede tener diferentes números de partes según el comando)
        String[] parts = message.split("\\|");
        if (parts.length == 0) {
            return;
        }
        String command = parts[0];

        try {
            // Usar Strategy Pattern para procesar comandos
            MessageHandler handler = handlerRegistry.getHandler(command);

            if (handler != null) {
                // Verificar si requiere autenticación
                if (handler.requiresAuthentication() && !authenticated) {
                    sendError("Debes iniciar sesión primero");
                    return;
                }

                // Procesar con el handler
                handler.handle(parts, this);
            } else {
                sendError("Comando desconocido: " + command);
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            sendError("Error procesando tu solicitud");
        }
    }
    
    private void receiveVideo() {
        try {
            while (videoActive && running && videoRecipient != null) {
                int length = videoIn.readInt(); // tamaño del frame
                byte[] frame = new byte[length];
                videoIn.readFully(frame);
                // Enviar video solo al destinatario privado
                boolean sent = server.sendPrivateVideo(frame, videoRecipient, this);
                if (!sent) {
                    System.out.println("Error: No se pudo enviar video a " + videoRecipient);
                    videoActive = false;
                    sendError("No se pudo enviar video. El destinatario puede haberse desconectado.");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Video finalizado para " + username);
        }
    }

    public void sendMessage(String message) {
        try {
            if (dataOut != null && !socket.isClosed()) {
                dataOut.writeUTF(message);
                dataOut.flush();
            }
        } catch (IOException e) {
            System.err.println("Error enviando mensaje a " + username + ": " + e.getMessage());
        }
    }


    public void sendFile(String fileName, byte[] fileData) {
        try {
            // Enviar encabezado
            int fileSize = fileData.length;
            sendMessage(MessageBuilder.buildFileTransferMetadata(fileName, fileSize));
            
            // No necesitamos Thread.sleep aquí - el cliente leerá cuando esté listo
            // El flush() asegura que el mensaje se envíe inmediatamente
            
            // Enviar los bytes del archivo
            dataOut.write(fileData);
            dataOut.flush();

            System.out.println("Enviando archivo " + fileName + " a " + username);

        } catch (IOException e) {
            System.err.println("Error enviando archivo a " + username + ": " + e.getMessage());
        }
    }


    public void close() {
        running = false;
        videoActive = false; // Detener video si está activo
        server.removeClient(this);

        try {
            // Cerrar recursos de video primero
            if (videoIn != null) {
                try {
                    videoIn.close();
                } catch (IOException e) {
                    // Ignorar errores al cerrar videoIn
                }
            }
            if (videoClient != null && !videoClient.isClosed()) {
                try {
                    videoClient.close();
                } catch (IOException e) {
                    // Ignorar errores al cerrar videoClient
                }
            }
            
            // Cerrar recursos principales
            if (dataOut != null) {
                try {
                    dataOut.close();
                } catch (IOException e) {
                    // Ignorar errores al cerrar dataOut
                }
            }
            if (dataIn != null) {
                try {
                    dataIn.close();
                } catch (IOException e) {
                    // Ignorar errores al cerrar dataIn
                }
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando conexión: " + e.getMessage());
        }
    }

}
