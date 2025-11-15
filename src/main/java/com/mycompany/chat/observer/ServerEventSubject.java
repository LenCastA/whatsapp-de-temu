package com.mycompany.chat.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject del patrón Observer.
 * Gestiona la lista de observadores y notifica eventos del servidor.
 * 
 * Esta clase es thread-safe para permitir su uso en entornos multihilo.
 */
public class ServerEventSubject {
    private final List<ServerObserver> observers;
    
    /**
     * Constructor que inicializa la lista de observadores.
     */
    public ServerEventSubject() {
        // Usar CopyOnWriteArrayList para thread-safety
        this.observers = new CopyOnWriteArrayList<>();
    }
    
    /**
     * Registra un observador para recibir notificaciones de eventos.
     * 
     * @param observer observador a registrar
     */
    public void addObserver(ServerObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    /**
     * Elimina un observador de la lista de notificaciones.
     * 
     * @param observer observador a eliminar
     */
    public void removeObserver(ServerObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notifica a todos los observadores registrados sobre un evento.
     * 
     * @param eventData datos del evento a notificar
     */
    public void notifyObservers(ServerEventData eventData) {
        for (ServerObserver observer : observers) {
            try {
                observer.onServerEvent(eventData);
            } catch (Exception e) {
                // Log error pero continuar notificando a otros observadores
                System.err.println("Error notificando observador: " + e.getMessage());
            }
        }
    }
    
    /**
     * Obtiene el número de observadores registrados.
     * 
     * @return número de observadores
     */
    public int getObserverCount() {
        return observers.size();
    }
    
    /**
     * Elimina todos los observadores registrados.
     */
    public void clearObservers() {
        observers.clear();
    }
}

