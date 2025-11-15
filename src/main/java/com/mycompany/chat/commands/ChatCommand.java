package com.mycompany.chat.commands;

import java.util.Scanner;

import com.mycompany.chat.ChatClient;
import com.mycompany.chat.util.Constants;

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
        System.out.println("(Escribe 'volver' para regresar al menu)");
        System.out.println("NOTA: Los mensajes se envian en segundo plano.");
        System.out.println("      Puedes seguir escribiendo mientras se envian.\n");
        
        while (client.isRunning()) {
            System.out.print("Mensaje: ");
            String message = scanner.nextLine().trim();
            
            if (message.equalsIgnoreCase("volver")) {
                break;
            }
            
            if (!message.isEmpty()) {
                // Validar tamaño del mensaje antes de enviar
                if (message.length() > Constants.MAX_MESSAGE_LENGTH) {
                    System.out.println("[!] Mensaje demasiado largo (max " + 
                                     Constants.MAX_MESSAGE_LENGTH + " caracteres)");
                    continue;
                }
                // Enviar mensaje en hilo separado (no bloquea)
                client.sendMessage(Constants.CMD_MSG + Constants.PROTOCOL_SEPARATOR + 
                                 recipient + Constants.PROTOCOL_SEPARATOR + message);
                System.out.println("[MENSAJE] Enviando mensaje en segundo plano...");
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Enviar mensaje de chat";
    }
}

