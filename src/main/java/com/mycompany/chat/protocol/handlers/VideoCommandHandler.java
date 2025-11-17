package com.mycompany.chat.protocol.handlers;

import com.mycompany.chat.ClientHandler;
import com.mycompany.chat.protocol.MessageHandler;
import com.mycompany.chat.util.Constants;

/**
 * Handler para el comando VIDEO usando Strategy Pattern.
 */
public class VideoCommandHandler implements MessageHandler {

    @Override
    public boolean handle(String[] parts, ClientHandler handler) {
        if (parts.length < 2) {
            handler.sendError("Formato incorrecto. Usa: VIDEO|START|destinatario o VIDEO|STOP");
            return false;
        }

        String action = parts[1];
        switch (action) {
            case "START":
                return handleStart(parts, handler);
            case "STOP":
                return handleStop(handler);
            default:
                handler.sendError("Acción de video desconocida: " + action);
                return false;
        }
    }

    private boolean handleStart(String[] parts, ClientHandler handler) {
        if (parts.length < 3) {
            handler.sendError("Formato incorrecto. Usa: VIDEO|START|destinatario");
            return false;
        }

        String recipient = parts[2];
        if (handler.getServer().getClientByUsername(recipient) == null) {
            handler.sendError("Usuario '" + recipient + "' no encontrado o no está conectado");
            return false;
        }

        if (handler.getVideoActive()) {
            handler.sendError("Ya hay una videollamada activa. Detén la actual primero.");
            return false;
        }

        try {
            handler.startVideoStream(recipient);
            handler.sendServerMessage("Videollamada iniciada con " + recipient);
            return true;
        } catch (IllegalStateException e) {
            handler.sendError(e.getMessage());
            return false;
        } catch (Exception e) {
            handler.sendError("No se pudo iniciar el video: " + e.getMessage());
            return false;
        }
    }

    private boolean handleStop(ClientHandler handler) {
        if (!handler.getVideoActive()) {
            handler.sendServerMessage("No hay videollamadas activas.");
            return true;
        }
        handler.stopVideoStream();
        handler.sendServerMessage("Videollamada detenida.");
        return true;
    }

    @Override
    public String getCommandName() {
        return Constants.CMD_VIDEO;
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
