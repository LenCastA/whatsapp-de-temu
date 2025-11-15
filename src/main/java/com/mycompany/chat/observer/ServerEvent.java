package com.mycompany.chat.observer;

/**
 * Enum que representa los diferentes tipos de eventos del servidor.
 * Utilizado en el patrón Observer para identificar el tipo de evento.
 */
public enum ServerEvent {
    /**
     * Evento cuando un usuario se conecta al servidor.
     */
    USER_CONNECTED,
    
    /**
     * Evento cuando un usuario se desconecta del servidor.
     */
    USER_DISCONNECTED,
    
    /**
     * Evento cuando se envía un mensaje privado.
     */
    PRIVATE_MESSAGE_SENT,
    
    /**
     * Evento cuando se envía un archivo.
     */
    FILE_SENT,
    
    /**
     * Evento cuando se inicia una videollamada.
     */
    VIDEO_CALL_STARTED,
    
    /**
     * Evento cuando se detiene una videollamada.
     */
    VIDEO_CALL_STOPPED
}

