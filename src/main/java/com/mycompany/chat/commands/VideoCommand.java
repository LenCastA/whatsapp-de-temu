package com.mycompany.chat.commands;

import com.mycompany.chat.ChatClient;
import java.util.Scanner;

/**
 * Comando para iniciar videollamadas privadas.
 * Implementa el patrón Command para encapsular la funcionalidad de video.
 */
public class VideoCommand implements Command {
    private final ChatClient client;
    private final Scanner scanner;
    
    public VideoCommand(ChatClient client, Scanner scanner) {
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
        System.out.println("            VIDELLAMADA");
        System.out.println("----------------------------------------------------");
        System.out.println("Destinatario: " + recipient);
        
        if (client.isVideoActive()) {
            System.out.println("[!] Ya hay una videollamada activa. Deten la actual primero.\n");
            return false;
        }
        
        System.out.println("\nIniciando videollamada con " + recipient + "...");
        client.startVideoCall(recipient);
        System.out.println("[VIDEO] Videollamada activada. Escribe 'detener' para finalizar.\n");
        
        // Esperar comando para detener
        while (client.isVideoActive() && client.isRunning()) {
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("detener") || input.equalsIgnoreCase("stop")) {
                client.stopVideoCall();
                System.out.println("[VIDEO DETENIDO] Videollamada detenida.\n");
                break;
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Iniciar videollamada";
    }
}

