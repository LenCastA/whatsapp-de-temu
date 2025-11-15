package com.mycompany.chat.commands;

import java.util.Scanner;

import com.mycompany.chat.ChatClient;

/**
 * Comando para enviar archivos de forma privada.
 * Implementa el patrón Command para encapsular la funcionalidad de envío de archivos.
 */
public class FileCommand implements Command {
    private final ChatClient client;
    private final Scanner scanner;
    
    public FileCommand(ChatClient client, Scanner scanner) {
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
        System.out.println("              ENVIAR ARCHIVO");
        System.out.println("----------------------------------------------------");
        System.out.println("Destinatario: " + recipient);
        System.out.println("NOTA: Los archivos se envian en segundo plano.");
        System.out.println("      Puedes seguir usando el chat mientras se envian.");
        System.out.println("(Escribe 'volver' en cualquier momento para regresar al menu)\n");
        
        while (client.isRunning()) {
            System.out.print("Ruta del archivo: ");
            String filePath = scanner.nextLine().trim();
            
            if (filePath.equalsIgnoreCase("volver")) {
                break;
            }
            
            if (filePath.isEmpty()) {
                System.out.println("[!] Ruta de archivo no valida.\n");
                continue;
            }
            
            // Enviar archivo en hilo separado (no bloquea)
            client.sendFile(filePath);
            System.out.println("[ARCHIVO] Iniciando envio de archivo en segundo plano...\n");
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return "Enviar archivo";
    }
}

