package com.mycompany.chat.service;

import com.mycompany.chat.ChatServer;
import com.mycompany.chat.security.InputValidator;

/**
 * Servicio para operaciones relacionadas con el servidor.
 * Separa la lógica de negocio de la presentación.
 */
public class ServerService {
    
    /**
     * Inicia el servidor de chat en el puerto especificado.
     * 
     * @param portStr string del puerto (puede estar vacío para usar default)
     * @return instancia del servidor iniciado, o null si hubo error
     */
    public ChatServer iniciarServidor(String portStr) {
        int port = InputValidator.validatePort(portStr);
        if (port == -1) {
            port = 9000;
        }
        
        try {
            ChatServer server = new ChatServer(port);
            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
            return server;
        } catch (Exception e) {
            System.err.println("Error iniciando servidor: " + e.getMessage());
            return null;
        }
    }
}

