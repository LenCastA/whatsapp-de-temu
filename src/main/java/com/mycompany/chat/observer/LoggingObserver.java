package com.mycompany.chat.observer;

/**
 * Observador que registra eventos en la consola.
 * 
 * Ejemplo de implementación del patrón Observer para logging.
 */
public class LoggingObserver implements ChatObserver {
    
    @Override
    public void onEvent(ChatEvent event) {
        // En una implementación real, esto podría usar un framework de logging
        // como SLF4J o Log4j
        System.out.println("[EVENTO] " + event.toString());
    }
}

