package com.mycompany.chat.protocol;

import com.mycompany.chat.protocol.handlers.FileCommandHandler;
import com.mycompany.chat.protocol.handlers.LoginHandler;
import com.mycompany.chat.protocol.handlers.LogoutHandler;
import com.mycompany.chat.protocol.handlers.MessageCommandHandler;
import com.mycompany.chat.protocol.handlers.UsersCommandHandler;
import com.mycompany.chat.protocol.handlers.VideoCommandHandler;
import com.mycompany.chat.service.DatabaseService;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry para los handlers de comandos usando Strategy Pattern.
 * Centraliza el registro y búsqueda de handlers.
 * 
 * Este patrón permite:
 * - Registrar nuevos handlers fácilmente
 * - Buscar handlers por nombre de comando
 * - Extensibilidad sin modificar código existente
 */
public class MessageHandlerRegistry {
    private final Map<String, MessageHandler> handlers = new HashMap<>();
    
    public MessageHandlerRegistry() {
        this(new DatabaseService());
    }

    public MessageHandlerRegistry(DatabaseService databaseService) {
        registerDefaultHandlers(databaseService);
    }
    
    /**
     * Registra los handlers por defecto del sistema.
     */
    private void registerDefaultHandlers(DatabaseService databaseService) {
        registerHandler(new LoginHandler(databaseService));
        registerHandler(new MessageCommandHandler());
        registerHandler(new UsersCommandHandler());
        registerHandler(new LogoutHandler());
        registerHandler(new FileCommandHandler());
        registerHandler(new VideoCommandHandler());
    }
    
    /**
     * Registra un nuevo handler.
     * 
     * @param handler El handler a registrar
     */
    public void registerHandler(MessageHandler handler) {
        handlers.put(handler.getCommandName(), handler);
    }
    
    /**
     * Obtiene un handler por nombre de comando.
     * 
     * @param commandName Nombre del comando
     * @return El handler correspondiente, o null si no existe
     */
    public MessageHandler getHandler(String commandName) {
        return handlers.get(commandName);
    }
    
    /**
     * Verifica si existe un handler para un comando.
     * 
     * @param commandName Nombre del comando
     * @return true si existe un handler para el comando
     */
    public boolean hasHandler(String commandName) {
        return handlers.containsKey(commandName);
    }
}

