package com.mycompany.chat.protocol.handlers;

import com.mycompany.chat.ClientHandler;
import com.mycompany.chat.protocol.MessageHandler;
import com.mycompany.chat.util.Constants;

/**
 * Handler para el comando MSG usando Strategy Pattern.
 */
public class MessageCommandHandler implements MessageHandler {
    
    @Override
    public boolean handle(String[] parts, ClientHandler handler) {
        if (!handler.isAuthenticated()) {
            handler.sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Debes iniciar sesión primero");
            return false;
        }
        
        if (parts.length < 3) {
            handler.sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Formato incorrecto. Usa: MSG|destinatario|mensaje");
            return false;
        }
        
        String recipient = parts[1];
        String msg = parts[2];
        
        // Validar tamaño del mensaje
        if (msg.length() > Constants.MAX_MESSAGE_LENGTH) {
            handler.sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Mensaje demasiado largo (max " + Constants.MAX_MESSAGE_LENGTH + " caracteres)");
            return false;
        }
        
        System.out.println("[" + handler.getUsername() + " -> " + recipient + "]: " + msg);
        
        // Enviar mensaje privado
        String messageToSend = Constants.CMD_MSG + Constants.PROTOCOL_SEPARATOR + 
                              handler.getUsername() + Constants.PROTOCOL_SEPARATOR + msg;
        boolean sent = handler.getServer().sendPrivateMessage(messageToSend, recipient, handler);
        if (!sent) {
            handler.sendMessage(Constants.RESP_ERROR + Constants.PROTOCOL_SEPARATOR + 
                       "Usuario '" + recipient + "' no encontrado o no está conectado");
            return false;
        } else {
            // Confirmar al emisor que el mensaje fue enviado
            handler.sendMessage(Constants.RESP_OK + Constants.PROTOCOL_SEPARATOR + 
                       Constants.CMD_MSG + Constants.PROTOCOL_SEPARATOR + 
                       "Mensaje enviado a " + recipient);
            return true;
        }
    }
    
    @Override
    public String getCommandName() {
        return Constants.CMD_MSG;
    }
    
    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}

