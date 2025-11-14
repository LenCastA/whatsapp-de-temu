package com.mycompany.chat.commands;

/**
 * Interfaz Command del patrón de diseño Command.
 * Encapsula una solicitud como un objeto, permitiendo parametrizar
 * clientes con diferentes solicitudes, encolar solicitudes y soportar
 * operaciones que pueden ser deshechas.
 */
public interface Command {
    /**
     * Ejecuta el comando.
     * @return true si el comando se ejecutó exitosamente, false en caso contrario
     */
    boolean execute();
    
    /**
     * Obtiene la descripción del comando para mostrar en el menú.
     * @return descripción del comando
     */
    String getDescription();
}

