package com.mycompany.chat;

import java.io.*;
import java.net.*;
import java.sql.SQLException;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ChatServer server;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dataIn;
    private DataInputStream videoIn;
    private DataOutputStream dataOut;
    private Socket videoClient;
    private String username;
    private boolean videoActive = false;
    private boolean authenticated;
    private volatile boolean running;
    
    public Socket getSocket(){return socket;}
    public Socket getVideoSocket(){return videoClient;}
    public boolean getVideoActive(){return videoActive;}

    public ClientHandler(Socket socket, ChatServer server, Socket videoSocket) {
        this.socket = socket;
        this.videoClient = videoSocket;
        this.server = server;
        this.authenticated = false;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            // Inicializar streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());

            sendMessage("SERVER|Bienvenido al servidor de chat. Por favor inicia sesión.");

            String line;
            while (running && (line = in.readLine()) != null) {
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

        String[] parts = message.split("\\|", 3);
        String command = parts[0];

        try {
            switch (command) {
                case "LOGIN":
                    handleLogin(parts);
                    break;

                case "MSG":
                    if (authenticated && parts.length >= 2) {
                        String msg = parts[1];
                        System.out.println("[" + username + "]: " + msg);
                        server.broadcast("MSG|" + username + "|" + msg, this);
                    } else if (!authenticated) {
                        sendMessage("ERROR|Debes iniciar sesión primero");
                    }
                    break;
                case "VIDEO":
                    handleVideoCommand();
                    break;
                case "FILE":
                     if (!authenticated) {
                        sendMessage("ERROR|Debes iniciar sesión primero");
                        return;
                    }

                    // Validar formato
                    if (parts.length < 3) {
                        sendMessage("ERROR|Formato incorrecto. Usa: FILE|nombre|tamaño");
                        return;
                    }

                    String fileName = parts[1];
                    int fileSize;
                    try {
                        fileSize = Integer.parseInt(parts[2]);
                    } catch (NumberFormatException e) {
                        sendMessage("ERROR|Tamaño de archivo inválido");
                        return;
                    }

                    try {
                        // Recibir bytes del archivo
                        byte[] fileData = new byte[fileSize];
                        dataIn.readFully(fileData);

                        System.out.println("Archivo recibido de " + username + ": " + fileName + " (" + fileSize + " bytes)");

                        // Notificar a otros clientes
                        server.broadcastFile(fileName, fileData, this);

                        sendMessage("SERVER|Archivo " + fileName + " enviado correctamente");

                    } catch (IOException e) {
                        sendMessage("ERROR|Error al recibir el archivo");
                        System.err.println("Error al recibir archivo de " + username + ": " + e.getMessage());
                    }
                    break;

                case "LOGOUT":
                    if (authenticated) {
                        sendMessage("SERVER|Cerrando sesión...");
                        running = false;
                    }
                    break;

                default:
                    sendMessage("ERROR|Comando desconocido: " + command);
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            sendMessage("ERROR|Error procesando tu solicitud");
        }
    }

    
private void handleLogin(String[] parts) {
    if (authenticated) {
        sendMessage("ERROR|Ya estás autenticado como " + username);
        return;
    }

    if (parts.length < 3) {
        sendMessage("ERROR|Formato incorrecto. Usa: LOGIN|username|password");
        return;
    }

    String user = parts[1];
    String pass = parts[2];

    try {
        if (Database.authenticate(user, pass)) {
            this.username = user;
            this.authenticated = true;
            server.addClient(this);
            sendMessage("OK|LOGIN|Bienvenido " + username + "!");
            server.broadcast("SYSTEM|" + username + " se ha conectado", this);
        } else {
            sendMessage("ERROR|Credenciales invalidas");
            System.out.println("Intento de login fallido: " + user);
        }
    } catch (Exception e) {
        sendMessage("ERROR|Error inesperado durante autenticacion");
        System.err.println("Error autenticando a " + user + ": " + e.getMessage());
    }
}
private void receiveVideo() {
    try {
        while (videoActive && running) {
            int length = videoIn.readInt(); // tamaño del frame
            byte[] frame = new byte[length];
            videoIn.readFully(frame);
            server.broadcastVideo(frame, this);
        }
    } catch (IOException e) {
        System.out.println("Video finalizado para " + username);
    }
}
private void handleVideoCommand() {
    if (!videoActive) {
        try {
            // ⚡ Inicializar video solo cuando se active
            videoIn = new DataInputStream(videoClient.getInputStream());
            videoActive = true;
            sendMessage("SERVER|Video activado.");
            new Thread(this::receiveVideo).start();
        } catch (IOException e) {
            sendMessage("ERROR|No se pudo iniciar el video: " + e.getMessage());
        }
    } else {
        videoActive = false;
        sendMessage("SERVER|Video desactivado.");
        try {
            if (videoIn != null) videoIn.close();
        } catch (IOException ignored) {}
    }
}
    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }


    public void sendFile(String fileName, byte[] fileData) {
        try {
            // Enviar encabezado
            int fileSize =fileData.length;
            sendMessage("FILE|" + fileName + "|" + fileSize);
            try {
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
            }
            
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
        server.removeClient(this);

        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (dataIn != null)
                dataIn.close();
            if (dataOut != null)
                dataOut.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando conexión: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
