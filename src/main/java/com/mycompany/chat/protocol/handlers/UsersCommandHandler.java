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
        List<String> users = handler.getServer().getConnectedUsers(handler);
        if (users.isEmpty()) {
            handler.sendServerMessage("No hay otros usuarios conectados");
        } else {
            StringBuilder userList = new StringBuilder("Usuarios conectados: ");
            for (int i = 0; i < users.size(); i++) {
                userList.append(users.get(i));
                if (i < users.size() - 1) {
                    userList.append(", ");
                }
            }
            handler.sendServerMessage(userList.toString());
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

