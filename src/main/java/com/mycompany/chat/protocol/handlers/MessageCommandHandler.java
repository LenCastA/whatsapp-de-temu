package com.mycompany.chat.protocol.handlers;

import com.mycompany.chat.ClientHandler;
import com.mycompany.chat.protocol.MessageBuilder;
import com.mycompany.chat.protocol.MessageHandler;
import com.mycompany.chat.util.Constants;

/**
 * Handler para el comando MSG usando Strategy Pattern.
 */
public class MessageCommandHandler implements MessageHandler {
    
    @Override
    public boolean handle(String[] parts, ClientHandler handler) {
        if (parts.length < 3) {
            handler.sendError("Formato incorrecto. Usa: MSG|destinatario|mensaje");
            return false;
        }
        
        String recipient = parts[1];
        String msg = parts[2];
        
        // Validar tamaño del mensaje
        if (msg.length() > Constants.MAX_MESSAGE_LENGTH) {
            handler.sendError("Mensaje demasiado largo (max " + Constants.MAX_MESSAGE_LENGTH + " caracteres)");
            return false;
        }
        
        System.out.println("[" + handler.getUsername() + " -> " + recipient + "]: " + msg);
        
        // Enviar mensaje privado
        String messageToSend = MessageBuilder.create()
                .withType(Constants.CMD_MSG)
                .withParams(handler.getUsername(), msg)
                .build();
        boolean sent = handler.getServer().sendPrivateMessage(messageToSend, recipient, handler);
        if (!sent) {
            handler.sendError("Usuario '" + recipient + "' no encontrado o no está conectado");
            return false;
        } else {
            handler.sendOk(Constants.CMD_MSG, "Mensaje enviado a " + recipient);
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

