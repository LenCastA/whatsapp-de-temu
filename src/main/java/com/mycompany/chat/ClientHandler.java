package com.mycompany.chat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

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
    public String getUsername(){return username;}
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

        // Parsear comando (puede tener diferentes números de partes según el comando)
        String[] parts = message.split("\\|");
        if (parts.length == 0) {
            return;
        }
        String command = parts[0];

        try {
            switch (command) {
                case "LOGIN":
                    handleLogin(parts);
                    break;

                case "MSG":
                    if (authenticated && parts.length >= 3) {
                        // Formato: MSG|destinatario|mensaje
                        String recipient = parts[1];
                        String msg = parts[2];
                        System.out.println("[" + username + " -> " + recipient + "]: " + msg);
                        
                        // Enviar mensaje privado
                        boolean sent = server.sendPrivateMessage("MSG|" + username + "|" + msg, recipient, this);
                        if (!sent) {
                            sendMessage("ERROR|Usuario '" + recipient + "' no encontrado o no está conectado");
                        } else {
                            // Confirmar al emisor que el mensaje fue enviado
                            sendMessage("OK|MSG|Mensaje enviado a " + recipient);
                        }
                    } else if (!authenticated) {
                        sendMessage("ERROR|Debes iniciar sesión primero");
                    } else {
                        sendMessage("ERROR|Formato incorrecto. Usa: MSG|destinatario|mensaje");
                    }
                    break;
                    
                case "USERS":
                    if (authenticated) {
                        List<String> users = server.getConnectedUsers(this);
                        if (users.isEmpty()) {
                            sendMessage("SERVER|No hay otros usuarios conectados");
                        } else {
                            StringBuilder userList = new StringBuilder("Usuarios conectados: ");
                            for (int i = 0; i < users.size(); i++) {
                                userList.append(users.get(i));
                                if (i < users.size() - 1) {
                                    userList.append(", ");
                                }
                            }
                            sendMessage("SERVER|" + userList.toString());
                        }
                    } else {
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

                    // Validar formato: FILE|destinatario|nombre|tamaño
                    if (parts.length < 4) {
                        sendMessage("ERROR|Formato incorrecto. Usa: FILE|destinatario|nombre|tamaño");
                        return;
                    }

                    String recipient = parts[1];
                    String fileName = parts[2];
                    int fileSize;
                    try {
                        fileSize = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException e) {
                        sendMessage("ERROR|Tamaño de archivo inválido");
                        return;
                    }

                    try {
                        // Recibir bytes del archivo
                        byte[] fileData = new byte[fileSize];
                        dataIn.readFully(fileData);

                        System.out.println("Archivo recibido de " + username + " para " + recipient + ": " + fileName + " (" + fileSize + " bytes)");

                        // Enviar archivo privado
                        boolean sent = server.sendPrivateFile(fileName, fileData, recipient, this);
                        if (sent) {
                            sendMessage("SERVER|Archivo " + fileName + " enviado correctamente a " + recipient);
                        } else {
                            sendMessage("ERROR|Usuario '" + recipient + "' no encontrado o no está conectado");
                        }

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

    public boolean isAuthenticated() {
        return authenticated;
    }
}
