package com.mycompany.chat.commands;

import com.mycompany.chat.ChatClient;

/**
 * Comando para salir de la aplicación.
 * Implementa el patrón Command para encapsular la funcionalidad de salida.
 */
public class ExitCommand implements Command {
    private final ChatClient client;
    
    public ExitCommand(ChatClient client) {
        this.client = client;
    }
    
    @Override
    public boolean execute() {
        client.sendMessage("LOGOUT");
        client.setRunning(false);
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Salir";
    }
}

