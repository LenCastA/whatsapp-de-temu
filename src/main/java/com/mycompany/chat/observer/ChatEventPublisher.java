package com.mycompany.chat.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publisher de eventos usando Observer Pattern.
 * 
 * Permite registrar observadores y notificarles cuando ocurren eventos.
 * Thread-safe usando CopyOnWriteArrayList.
 */
public class ChatEventPublisher {
    private final List<ChatObserver> observers = new CopyOnWriteArrayList<>();
    
    /**
     * Registra un nuevo observador.
     * 
     * @param observer El observador a registrar
     */
    public void subscribe(ChatObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    /**
     * Elimina un observador.
     * 
     * @param observer El observador a eliminar
     */
    public void unsubscribe(ChatObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notifica a todos los observadores sobre un evento.
     * 
     * @param event El evento a notificar
     */
    public void notifyObservers(ChatEvent event) {
        for (ChatObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (Exception e) {
                // No permitir que un observador con errores afecte a los demás
                System.err.println("Error notificando a observador: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notifica un evento creándolo automáticamente.
     * 
     * @param type Tipo del evento
     * @param username Nombre de usuario relacionado (puede ser null)
     * @param message Mensaje del evento
     */
    public void publishEvent(ChatEvent.EventType type, String username, String message) {
        ChatEvent event = new ChatEvent(type, username, message);
        notifyObservers(event);
    }
    
    /**
     * Obtiene el número de observadores registrados.
     * 
     * @return Número de observadores
     */
    public int getObserverCount() {
        return observers.size();
    }
}

