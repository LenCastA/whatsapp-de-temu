package com.mycompany.chat.observer;

import java.time.LocalDateTime;

/**
 * Clase que representa un evento del sistema de chat.
 * 
 * Usado en el patrón Observer para notificar eventos importantes.
 */
public class ChatEvent {
    public enum EventType {
        USER_CONNECTED,
        USER_DISCONNECTED,
        MESSAGE_SENT,
        FILE_SENT,
        VIDEO_STARTED,
        VIDEO_STOPPED,
        LOGIN_SUCCESS,
        LOGIN_FAILED
    }
    
    private final EventType type;
    private final String username;
    private final String message;
    private final LocalDateTime timestamp;
    
    public ChatEvent(EventType type, String username, String message) {
        this.type = type;
        this.username = username;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public EventType getType() {
        return type;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getMessage() {
        return message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s", 
            timestamp, type, username != null ? username : "System", message);
    }
}

