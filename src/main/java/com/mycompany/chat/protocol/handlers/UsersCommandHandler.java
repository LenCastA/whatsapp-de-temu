package com.mycompany.chat.protocol.handlers;

import com.mycompany.chat.ClientHandler;
import com.mycompany.chat.protocol.MessageHandler;
import com.mycompany.chat.util.Constants;
import java.util.List;

/**
 * Handler para el comando USERS usando Strategy Pattern.
 */
public class UsersCommandHandler implements MessageHandler {
    
    @Override
    public boolean handle(String[] parts, ClientHandler handler) {
        if (!handler.isAuthenticated()) {
            handler.sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Debes iniciar sesión primero");
            return false;
        }
        
        List<String> users = handler.getServer().getConnectedUsers(handler);
        if (users.isEmpty()) {
            handler.sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                       "No hay otros usuarios conectados");
        } else {
            StringBuilder userList = new StringBuilder("Usuarios conectados: ");
            for (int i = 0; i < users.size(); i++) {
                userList.append(users.get(i));
                if (i < users.size() - 1) {
                    userList.append(", ");
                }
            }
            handler.sendMessage(Constants.RESP_SERVER + Constants.PROTOCOL_SEPARATOR + 
                      userList.toString());
        }
        return true;
    }
    
    @Override
    public String getCommandName() {
        return Constants.CMD_USERS;
    }
    
    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}

