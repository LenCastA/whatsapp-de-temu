package com.mycompany.chat.protocol.handlers;

import com.mycompany.chat.ClientHandler;
import com.mycompany.chat.protocol.MessageHandler;
import com.mycompany.chat.service.DatabaseService;
import com.mycompany.chat.util.Constants;

/**
 * Handler para el comando LOGIN usando Strategy Pattern.
 */
public class LoginHandler implements MessageHandler {
    private final DatabaseService databaseService;

    public LoginHandler(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
    
    @Override
    public boolean handle(String[] parts, ClientHandler handler) {
        if (handler.isAuthenticated()) {
            handler.sendError("Ya estás autenticado como " + handler.getUsername());
            return true;
        }

        if (parts.length < 3) {
            handler.sendError("Formato incorrecto. Usa: LOGIN|username|password");
            return true;
        }

        String user = parts[1];
        String pass = parts[2];

        try {
            if (databaseService.autenticarUsuario(user, pass)) {
                handler.setUsername(user);
                handler.setAuthenticated(true);
                handler.getServer().addClient(handler);
                handler.sendOk(Constants.CMD_LOGIN, "Bienvenido " + user + "!");
                handler.getServer().broadcast(Constants.RESP_SYSTEM + Constants.PROTOCOL_SEPARATOR +
                            user + " se ha conectado", handler);
                return true;
            } else {
                handler.sendError("Credenciales inválidas");
                System.out.println("Intento de login fallido: " + user);
                return false;
            }
        } catch (Exception e) {
            handler.sendError("Error inesperado durante autenticacion");
            System.err.println("Error autenticando a " + user + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getCommandName() {
        return Constants.CMD_LOGIN;
    }
    
    @Override
    public boolean requiresAuthentication() {
        return false; // Login no requiere autenticación previa
    }
}

