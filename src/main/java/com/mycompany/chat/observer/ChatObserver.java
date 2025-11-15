package com.mycompany.chat.observer;

/**
 * Interfaz Observer para recibir notificaciones de eventos del chat.
 * 
 * Este patrón Observer permite:
 * - Desacoplar los componentes que generan eventos de los que los consumen
 * - Agregar nuevos observadores sin modificar código existente
 * - Implementar logging, estadísticas, notificaciones, etc.
 */
public interface ChatObserver {
    
    /**
     * Se llama cuando ocurre un evento en el sistema.
     * 
     * @param event El evento que ocurrió
     */
    void onEvent(ChatEvent event);
}

