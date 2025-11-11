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
    private DataOutputStream dataOut;
    private String username;
    private boolean authenticated;
    private volatile boolean running;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
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

                case "FILE":
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
        } catch (SQLException e) {
            sendMessage("ERROR|Error de base de datos");
            System.err.println("====================================");
            System.err.println("ERROR SQL EN AUTENTICACIÓN:");
            System.err.println("Usuario: " + user);
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            System.err.println("====================================");
        }
    }
    
    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    // Implementar tranferencias de archivos
    public void sendFile(String fileName, byte[] fileData) {
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
