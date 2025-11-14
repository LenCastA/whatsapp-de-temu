package com.mycompany.chat.commands;

import java.util.Map;
import java.util.HashMap;

/**
 * Invocador de comandos que gestiona y ejecuta los comandos del menú.
 * Implementa el patrón Command como invocador (Invoker).
 * 
 * Este patrón permite:
 * - Encapsular solicitudes como objetos
 * - Parametrizar objetos con diferentes solicitudes
 * - Facilita agregar nuevos comandos sin modificar código existente
 */
public class MenuCommandInvoker {
    private final Map<String, Command> commands;
    
    public MenuCommandInvoker() {
        this.commands = new HashMap<>();
    }
    
    /**
     * Registra un comando con una clave.
     * @param key clave del comando (ej: "1", "2", etc.)
     * @param command comando a registrar
     */
    public void registerCommand(String key, Command command) {
        commands.put(key, command);
    }
    
    /**
     * Ejecuta un comando por su clave.
     * @param key clave del comando
     * @return true si el comando se ejecutó exitosamente, false si no existe
     */
    public boolean executeCommand(String key) {
        Command command = commands.get(key);
        if (command != null) {
            return command.execute();
        }
        return false;
    }
    
    /**
     * Obtiene la descripción de un comando por su clave.
     * @param key clave del comando
     * @return descripción del comando o null si no existe
     */
    public String getCommandDescription(String key) {
        Command command = commands.get(key);
        return command != null ? command.getDescription() : null;
    }
    
    /**
     * Obtiene todos los comandos registrados.
     * @return mapa de comandos
     */
    public Map<String, Command> getCommands() {
        return new HashMap<>(commands);
    }
}

