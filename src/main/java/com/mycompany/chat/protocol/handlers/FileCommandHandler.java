package com.mycompany.chat.protocol.handlers;

import com.mycompany.chat.ClientHandler;
import com.mycompany.chat.protocol.MessageHandler;
import com.mycompany.chat.util.Constants;
import java.io.IOException;

/**
 * Handler para el comando FILE usando el Strategy Pattern.
 */
public class FileCommandHandler implements MessageHandler {

    @Override
    public boolean handle(String[] parts, ClientHandler handler) {
        if (parts.length < 4) {
            handler.sendError("Formato incorrecto. Usa: FILE|destinatario|nombre|tamaño");
            return false;
        }

        String recipient = parts[1];
        String fileName = parts[2];
        int fileSize;
        try {
            fileSize = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            handler.sendError("Tamaño de archivo inválido");
            return false;
        }

        if (fileSize > Constants.MAX_FILE_SIZE_BYTES) {
            handler.sendError("Archivo demasiado grande (max " + Constants.MAX_FILE_SIZE_MB + "MB)");
            return false;
        }

        try {
            byte[] fileData = handler.readBytes(fileSize);
            System.out.println("Archivo recibido de " + handler.getUsername() + " para " + recipient + ": " + fileName +
                               " (" + fileSize + " bytes)");

            boolean sent = handler.getServer().sendPrivateFile(fileName, fileData, recipient, handler);
            if (sent) {
                handler.sendServerMessage("Archivo " + fileName + " enviado correctamente a " + recipient);
                return true;
            }
            handler.sendError("Usuario '" + recipient + "' no encontrado o no está conectado");
            return false;
        } catch (IOException e) {
            handler.sendError("Error al recibir el archivo");
            System.err.println("Error al recibir archivo de " + handler.getUsername() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getCommandName() {
        return Constants.CMD_FILE;
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }
}
