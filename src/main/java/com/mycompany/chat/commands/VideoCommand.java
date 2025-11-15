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
            System.out.println("[!] Ya hay una videollamada activa.");
            System.out.println("NOTA: El video se transmite en segundo plano.");
            System.out.println("      Puedes enviar mensajes y archivos mientras el video esta activo.");
            System.out.println("(Escribe 'volver' para regresar al menu o 'detener' para finalizar el video)\n");
        } else {
            System.out.println("\nIniciando videollamada con " + recipient + "...");
            client.startVideoCall(recipient);
            System.out.println("[VIDEO] Videollamada activada.");
            System.out.println("NOTA: El video se transmite en segundo plano.");
            System.out.println("      Puedes enviar mensajes y archivos mientras el video esta activo.");
            System.out.println("(Escribe 'volver' para regresar al menu o 'detener' para finalizar el video)\n");
        }
        
        // Permitir volver al menú o detener el video
        while (client.isRunning()) {
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("volver")) {
                // Volver al menú sin detener el video (si está activo)
                if (client.isVideoActive()) {
                    System.out.println("[VIDEO] Videollamada continua en segundo plano.");
                    System.out.println("        Puedes enviar mensajes y archivos desde el menu.\n");
                }
                break;
            } else if (input.equalsIgnoreCase("detener") || input.equalsIgnoreCase("stop")) {
                if (client.isVideoActive()) {
                    client.stopVideoCall();
                    System.out.println("[VIDEO DETENIDO] Videollamada detenida.\n");
                } else {
                    System.out.println("[!] No hay videollamada activa.\n");
                }
                break;
            } else if (!input.isEmpty()) {
                System.out.println("[!] Comando no reconocido. Escribe 'volver' o 'detener'.\n");
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Iniciar videollamada";
    }
}

