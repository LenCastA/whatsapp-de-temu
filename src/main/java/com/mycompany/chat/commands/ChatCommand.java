package com.mycompany.chat.commands;

import com.mycompany.chat.ChatClient;
import java.util.Scanner;

/**
 * Comando para enviar mensajes de chat privados.
 * Implementa el patrón Command para encapsular la funcionalidad de chat.
 */
public class ChatCommand implements Command {
    private final ChatClient client;
    private final Scanner scanner;
    
    public ChatCommand(ChatClient client, Scanner scanner) {
        this.client = client;
        this.scanner = scanner;
    }
    
    @Override
    public boolean execute() {
        String recipient = client.getCurrentRecipient();
        if (recipient == null || recipient.isEmpty()) {
            System.out.println("[!] Error: No hay destinatario seleccionado.");
            return false;
        }
        
        System.out.println("\n----------------------------------------------------");
        System.out.println("           ENVIAR MENSAJE DE CHAT");
        System.out.println("----------------------------------------------------");
        System.out.println("Destinatario: " + recipient);
        System.out.println("(Escribe 'volver' para regresar al menu)\n");
        
        while (client.isRunning() && recipient != null) {
            System.out.print("Mensaje: ");
            String message = scanner.nextLine().trim();
            
            if (message.equalsIgnoreCase("volver")) {
                break;
            }
            
            if (!message.isEmpty()) {
                client.sendMessage("MSG|" + recipient + "|" + message);
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Enviar mensaje de chat";
    }
}

