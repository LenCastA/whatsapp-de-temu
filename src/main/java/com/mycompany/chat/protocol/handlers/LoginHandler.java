package com.mycompany.chat.protocol.handlers;

import com.mycompany.chat.ClientHandler;
import com.mycompany.chat.Database;
import com.mycompany.chat.protocol.MessageHandler;
import com.mycompany.chat.util.Constants;

/**
 * Handler para el comando LOGIN usando Strategy Pattern.
 */
public class LoginHandler implements MessageHandler {
    
    @Override
    public boolean handle(String[] parts, ClientHandler handler) {
        if (handler.isAuthenticated()) {
            handler.sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Ya estás autenticado como " + handler.getUsername());
            return true;
        }

        if (parts.length < 3) {
            handler.sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Formato incorrecto. Usa: LOGIN|username|password");
            return true;
        }

        String user = parts[1];
        String pass = parts[2];

        try {
            if (Database.authenticate(user, pass)) {
                handler.setUsername(user);
                handler.setAuthenticated(true);
                handler.getServer().addClient(handler);
                handler.sendMessage(Constants.RESP_OK + Constants.PROTOCOL_SEPARATOR + 
                           Constants.CMD_LOGIN + Constants.PROTOCOL_SEPARATOR + 
                           "Bienvenido " + user + "!");
                handler.getServer().broadcast(Constants.RESP_SYSTEM + Constants.PROTOCOL_SEPARATOR + 
                            user + " se ha conectado", handler);
                return true;
            } else {
                handler.sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                           "Credenciales invalidas");
                System.out.println("Intento de login fallido: " + user);
                return false;
            }
        } catch (Exception e) {
            handler.sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Error inesperado durante autenticacion");
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

