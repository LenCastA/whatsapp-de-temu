package com.mycompany.chat.observer;

/**
 * Interfaz Observer del patrón Observer.
 * Define el método que será llamado cuando ocurra un evento en el servidor.
 * 
 * Este patrón permite:
 * - Desacoplar el servidor de los componentes que necesitan reaccionar a eventos
 * - Notificar múltiples observadores de eventos del servidor
 * - Facilitar la extensión con nuevos tipos de observadores
 */
public interface ServerObserver {
    /**
     * Método llamado cuando ocurre un evento en el servidor.
     * 
     * @param eventData datos del evento que ocurrió
     */
    void onServerEvent(ServerEventData eventData);
}

