package com.mycompany.chat.service;

import com.mycompany.chat.ChatClient;
import com.mycompany.chat.security.InputValidator;

/**
 * Servicio para operaciones relacionadas con el cliente.
 * Separa la lógica de negocio de la presentación.
 */
public class ClientService {
    
    /**
     * Inicia el cliente de chat conectándose al servidor especificado.
     * 
     * @param host dirección IP o hostname del servidor
     * @param portStr string del puerto (puede estar vacío para usar default)
     * @return instancia del cliente iniciado, o null si hubo error
     */
    public ChatClient iniciarCliente(String host, String portStr) {
        // Validar host
        if (host == null || host.trim().isEmpty()) {
            host = "localhost";
        } else {
            String error = InputValidator.validateHost(host);
            if (error != null) {
                System.err.println("Error en host: " + error);
                return null;
            }
        }
        
        // Validar puerto
        int port = InputValidator.validatePort(portStr);
        if (port == -1) {
            port = 9000;
        }
        
        try {
            return new ChatClient(host, port);
        } catch (Exception e) {
            System.err.println("Error iniciando cliente: " + e.getMessage());
            return null;
        }
    }
}

