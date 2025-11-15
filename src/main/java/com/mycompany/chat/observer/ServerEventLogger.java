package com.mycompany.chat.observer;

/**
 * Observador concreto que registra eventos del servidor en la consola.
 * Implementa el patrón Observer para logging de eventos.
 * 
 * Este observador puede ser usado para:
 * - Logging de eventos del servidor
 * - Auditoría de acciones de usuarios
 * - Debugging y monitoreo
 */
public class ServerEventLogger implements ServerObserver {
    private final boolean verbose;
    
    /**
     * Constructor con logging estándar.
     */
    public ServerEventLogger() {
        this(false);
    }
    
    /**
     * Constructor con opción de logging detallado.
     * 
     * @param verbose si es true, muestra información detallada de los eventos
     */
    public ServerEventLogger(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public void onServerEvent(ServerEventData eventData) {
        if (eventData == null) {
            return;
        }
        
        ServerEvent eventType = eventData.getEventType();
        String username = eventData.getUsername();
        
        switch (eventType) {
            case USER_CONNECTED:
                if (verbose) {
                    System.out.println("[OBSERVER] Usuario conectado: " + username);
                }
                break;
                
            case USER_DISCONNECTED:
                if (verbose) {
                    System.out.println("[OBSERVER] Usuario desconectado: " + username);
                }
                break;
                
            case PRIVATE_MESSAGE_SENT:
                if (verbose) {
                    String recipient = eventData.getRecipient();
                    System.out.println("[OBSERVER] Mensaje enviado de " + username + " a " + recipient);
                }
                break;
                
            case FILE_SENT:
                if (verbose) {
                    String recipient = eventData.getRecipient();
                    System.out.println("[OBSERVER] Archivo enviado de " + username + " a " + recipient);
                }
                break;
                
            case VIDEO_CALL_STARTED:
                if (verbose) {
                    String recipient = eventData.getRecipient();
                    System.out.println("[OBSERVER] Videollamada iniciada entre " + username + " y " + recipient);
                }
                break;
                
            case VIDEO_CALL_STOPPED:
                if (verbose) {
                    String recipient = eventData.getRecipient();
                    System.out.println("[OBSERVER] Videollamada detenida entre " + username + " y " + recipient);
                }
                break;
        }
    }
}

