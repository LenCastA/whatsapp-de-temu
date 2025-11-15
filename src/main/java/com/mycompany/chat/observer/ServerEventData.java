package com.mycompany.chat.observer;

/**
 * Clase que contiene los datos asociados a un evento del servidor.
 * Utilizada en el patrón Observer para pasar información a los observadores.
 */
public class ServerEventData {
    private final ServerEvent eventType;
    private final String username;
    private final String recipient;
    private final String message;
    private final Object additionalData;
    
    /**
     * Constructor para eventos simples.
     * 
     * @param eventType tipo de evento
     * @param username nombre de usuario relacionado con el evento
     */
    public ServerEventData(ServerEvent eventType, String username) {
        this(eventType, username, null, null, null);
    }
    
    /**
     * Constructor para eventos con destinatario.
     * 
     * @param eventType tipo de evento
     * @param username nombre de usuario que genera el evento
     * @param recipient destinatario del evento
     */
    public ServerEventData(ServerEvent eventType, String username, String recipient) {
        this(eventType, username, recipient, null, null);
    }
    
    /**
     * Constructor completo.
     * 
     * @param eventType tipo de evento
     * @param username nombre de usuario que genera el evento
     * @param recipient destinatario del evento (puede ser null)
     * @param message mensaje asociado (puede ser null)
     * @param additionalData datos adicionales (puede ser null)
     */
    public ServerEventData(ServerEvent eventType, String username, String recipient, 
                          String message, Object additionalData) {
        this.eventType = eventType;
        this.username = username;
        this.recipient = recipient;
        this.message = message;
        this.additionalData = additionalData;
    }
    
    public ServerEvent getEventType() {
        return eventType;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getRecipient() {
        return recipient;
    }
    
    public String getMessage() {
        return message;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getAdditionalData(Class<T> type) {
        if (additionalData != null && type.isInstance(additionalData)) {
            return (T) additionalData;
        }
        return null;
    }
}

