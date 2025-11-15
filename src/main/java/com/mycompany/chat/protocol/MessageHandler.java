package com.mycompany.chat.protocol;

import com.mycompany.chat.ClientHandler;

/**
 * Interfaz Strategy para el manejo de comandos del protocolo.
 * Cada comando implementa esta interfaz para procesar mensajes específicos.
 * 
 * Este patrón Strategy permite:
 * - Eliminar el switch grande en ClientHandler
 * - Facilitar la adición de nuevos comandos
 * - Separar la lógica de cada comando en clases independientes
 * - Mejorar la testabilidad
 */
public interface MessageHandler {
    
    /**
     * Procesa un mensaje del protocolo.
     * 
     * @param parts Array de partes del mensaje parseado (separado por |)
     * @param handler El ClientHandler que recibió el mensaje
     * @return true si el mensaje fue procesado correctamente
     */
    boolean handle(String[] parts, ClientHandler handler);
    
    /**
     * Obtiene el nombre del comando que este handler procesa.
     * 
     * @return Nombre del comando (ej: "LOGIN", "MSG", etc.)
     */
    String getCommandName();
    
    /**
     * Verifica si el handler requiere autenticación.
     * 
     * @return true si el comando requiere que el usuario esté autenticado
     */
    boolean requiresAuthentication();
}

