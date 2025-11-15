package com.mycompany.chat.protocol.handlers;

import com.mycompany.chat.ClientHandler;
import com.mycompany.chat.protocol.MessageHandler;
import com.mycompany.chat.util.Constants;

/**
 * Handler para el comando LOGOUT usando Strategy Pattern.
 */
public class LogoutHandler implements MessageHandler {
    
    @Override
    public boolean handle(String[] parts, ClientHandler handler) {
        if (handler.isAuthenticated()) {
            handler.sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                       "Cerrando sesión...");
            handler.setRunning(false);
            return true;
        }
        return false;
    }
    
    @Override
    public String getCommandName() {
        return Constants.CMD_LOGOUT;
    }
    
    @Override
    public boolean requiresAuthentication() {
        return false; // Logout puede ejecutarse sin autenticación
    }
}

