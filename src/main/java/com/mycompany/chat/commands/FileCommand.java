package com.mycompany.chat.commands;

import com.mycompany.chat.ChatClient;
import java.util.Scanner;

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
            System.out.println("⚠️  Error: No hay destinatario seleccionado.");
            return false;
        }
        
        System.out.println("\n───────────────────────────────────────────────────");
        System.out.println("              ENVIAR ARCHIVO");
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("Destinatario: " + recipient);
        System.out.print("Ruta del archivo (o 'volver' para regresar): ");
        
        String filePath = scanner.nextLine().trim();
        
        if (filePath.equalsIgnoreCase("volver")) {
            return true;
        }
        
        if (!filePath.isEmpty()) {
            client.sendFile(filePath);
            return true;
        } else {
            System.out.println("⚠️  Ruta de archivo no válida.\n");
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Enviar archivo";
    }
}

