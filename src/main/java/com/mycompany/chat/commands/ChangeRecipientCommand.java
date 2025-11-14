package com.mycompany.chat.commands;

import com.mycompany.chat.ChatClient;

/**
 * Comando para cambiar el destinatario actual.
 * Implementa el patrón Command para encapsular el cambio de destinatario.
 */
public class ChangeRecipientCommand implements Command {
    private final ChatClient client;
    
    public ChangeRecipientCommand(ChatClient client) {
        this.client = client;
    }
    
    @Override
    public boolean execute() {
        client.setCurrentRecipient(null);
        System.out.println("\nCambiando destinatario...\n");
        // El cliente manejará la selección de nuevo destinatario
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Cambiar destinatario";
    }
}

