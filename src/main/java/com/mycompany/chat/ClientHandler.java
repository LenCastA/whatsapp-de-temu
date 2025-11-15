package com.mycompany.chat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import com.mycompany.chat.util.Constants;
import com.mycompany.chat.observer.ServerEvent;
import com.mycompany.chat.observer.ServerEventData;

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
    private String videoRecipient = null; // Destinatario para video privado
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
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());

            sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                       "Bienvenido al servidor de chat. Por favor inicia sesión.");

            while (running) {
                String line =dataIn.readUTF();
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
                        
                        // Validar tamaño del mensaje
                        if (msg.length() > Constants.MAX_MESSAGE_LENGTH) {
                            sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                       "Mensaje demasiado largo (max " + Constants.MAX_MESSAGE_LENGTH + " caracteres)");
                            break;
                        }
                        
                        System.out.println("[" + username + " -> " + recipient + "]: " + msg);
                        
                        // Enviar mensaje privado
                        String messageToSend = Constants.CMD_MSG + Constants.PROTOCOL_SEPARATOR + 
                                              username + Constants.PROTOCOL_SEPARATOR + msg;
                        boolean sent = server.sendPrivateMessage(messageToSend, recipient, this);
                        if (!sent) {
                            sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                       "Usuario '" + recipient + "' no encontrado o no está conectado");
                        } else {
                            // Confirmar al emisor que el mensaje fue enviado
                            sendMessage(Constants.RESP_OK + Constants.PROTOCOL_SEPARATOR + 
                                       Constants.CMD_MSG + Constants.PROTOCOL_SEPARATOR + 
                                       "Mensaje enviado a " + recipient);
                        }
                    } else if (!authenticated) {
                        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                   "Debes iniciar sesión primero");
                    } else {
                        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                   "Formato incorrecto. Usa: MSG|destinatario|mensaje");
                    }
                    break;
                    
                case "USERS":
                    if (authenticated) {
                        List<String> users = server.getConnectedUsers(this);
                        if (users.isEmpty()) {
                            sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                                       "No hay otros usuarios conectados");
                        } else {
                            StringBuilder userList = new StringBuilder("Usuarios conectados: ");
                            for (int i = 0; i < users.size(); i++) {
                                userList.append(users.get(i));
                                if (i < users.size() - 1) {
                                    userList.append(", ");
                                }
                            }
                            sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                                      userList.toString());
                        }
                    } else {
                        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                   "Debes iniciar sesión primero");
                    }
                    break;
                case "VIDEO":
                    handleVideoCommand(parts);
                    break;
                case "FILE":
                     if (!authenticated) {
                        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                   "Debes iniciar sesión primero");
                        return;
                    }

                    // Validar formato: FILE|destinatario|nombre|tamaño
                    if (parts.length < 4) {
                        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                   "Formato incorrecto. Usa: FILE|destinatario|nombre|tamaño");
                        return;
                    }

                    String recipient = parts[1];
                    String fileName = parts[2];
                    int fileSize;
                    try {
                        fileSize = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException e) {
                        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                   "Tamaño de archivo inválido");
                        return;
                    }
                    
                    // Validar tamaño del archivo
                    if (fileSize > Constants.MAX_FILE_SIZE_BYTES) {
                        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                   "Archivo demasiado grande (max " + Constants.MAX_FILE_SIZE_MB + "MB)");
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
                            sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                                       "Archivo " + fileName + " enviado correctamente a " + recipient);
                        } else {
                            sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                       "Usuario '" + recipient + "' no encontrado o no está conectado");
                        }

                    } catch (IOException e) {
                        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                                   "Error al recibir el archivo");
                        System.err.println("Error al recibir archivo de " + username + ": " + e.getMessage());
                    }
                    break;

                case "LOGOUT":
                    if (authenticated) {
                        sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                                   "Cerrando sesión...");
                        running = false;
                    }
                    break;

                default:
                    sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                               "Comando desconocido: " + command);
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Error procesando tu solicitud");
        }
    }

    
private void handleLogin(String[] parts) {
    if (authenticated) {
        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                   "Ya estás autenticado como " + username);
        return;
    }

    if (parts.length < 3) {
        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                   "Formato incorrecto. Usa: LOGIN|username|password");
        return;
    }

    String user = parts[1];
    String pass = parts[2];

    try {
        if (Database.authenticate(user, pass)) {
            this.username = user;
            this.authenticated = true;
            server.addClient(this);
            sendMessage(Constants.RESP_OK + Constants.PROTOCOL_SEPARATOR + 
                       Constants.CMD_LOGIN + Constants.PROTOCOL_SEPARATOR + 
                       "Bienvenido " + username + "!");
            server.broadcast(Constants.RESP_SYSTEM + Constants.PROTOCOL_SEPARATOR + 
                            username + " se ha conectado", this);
        } else {
            sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Credenciales invalidas");
            System.out.println("Intento de login fallido: " + user);
        }
    } catch (Exception e) {
        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                   "Error inesperado durante autenticacion");
        System.err.println("Error autenticando a " + user + ": " + e.getMessage());
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
                sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                           "No se pudo enviar video. El destinatario puede haberse desconectado.");
                break;
            }
        }
    } catch (IOException e) {
        System.out.println("Video finalizado para " + username);
    }
}

private void handleVideoCommand(String[] parts) {
    if (!authenticated) {
        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                   "Debes iniciar sesión primero");
        return;
    }
    
    // Formato: VIDEO|START|destinatario o VIDEO|STOP
    if (parts.length >= 2 && "START".equals(parts[1])) {
        if (parts.length < 3) {
            sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Formato incorrecto. Usa: VIDEO|START|destinatario");
            return;
        }
        
        String recipient = parts[2];
        
        // Verificar que el destinatario existe
        if (server.getClientByUsername(recipient) == null) {
            sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Usuario '" + recipient + "' no encontrado o no está conectado");
            return;
        }
        
        if (!videoActive) {
            try {
                // Inicializar video solo cuando se active
                videoIn = new DataInputStream(videoClient.getInputStream());
                videoActive = true;
                videoRecipient = recipient;
                sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                           "Videollamada iniciada con " + recipient);
                // Observer Pattern: Notificar evento de videollamada iniciada
                if (username != null && server.getEventSubject() != null) {
                    server.getEventSubject().notifyObservers(new ServerEventData(
                        ServerEvent.VIDEO_CALL_STARTED, username, recipient));
                }
                new Thread(this::receiveVideo).start();
            } catch (IOException e) {
                sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                           "No se pudo iniciar el video: " + e.getMessage());
            }
        } else {
            sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Ya hay una videollamada activa. Detén la actual primero.");
        }
    } else if (parts.length >= 2 && "STOP".equals(parts[1])) {
        String previousRecipient = videoRecipient;
        videoActive = false;
        videoRecipient = null;
        sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                   "Videollamada detenida.");
        // Observer Pattern: Notificar evento de videollamada detenida
        if (username != null && previousRecipient != null && server.getEventSubject() != null) {
            server.getEventSubject().notifyObservers(new ServerEventData(
                ServerEvent.VIDEO_CALL_STOPPED, username, previousRecipient));
        }
        try {
            if (videoIn != null) videoIn.close();
        } catch (IOException ignored) {}
    } else {
        sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                   "Formato incorrecto. Usa: VIDEO|START|destinatario o VIDEO|STOP");
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
            sendMessage(Constants.CMD_FILE + Constants.PROTOCOL_SEPARATOR + 
                       fileName + Constants.PROTOCOL_SEPARATOR + fileSize);
            
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
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando conexión: " + e.getMessage());
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
