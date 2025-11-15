package com.mycompany.chat.commands;

import com.mycompany.chat.ChatClient;
import com.mycompany.chat.util.Constants;
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
        System.out.println("[VIDEO] Videollamada activada.");
        System.out.println("Comandos disponibles:");
        System.out.println("  - Escribe un mensaje para enviarlo a " + recipient);
        System.out.println("  - Escribe '/detener' o '/stop' para finalizar la videollamada\n");

        // Esperar comandos durante la videollamada
        while (client.isVideoActive() && client.isRunning()) {
            System.out.print("[VIDEO] > ");
            String input = scanner.nextLine().trim();

            // Verificar si el usuario quiere detener la videollamada
            if (input.equalsIgnoreCase("/detener") || input.equalsIgnoreCase("/stop")) {
                client.stopVideoCall();
                System.out.println("[VIDEO DETENIDO] Videollamada finalizada.\n");
                break;
            }

            // Si no está vacío y no es comando de detener, enviar como mensaje
            if (!input.isEmpty()) {
                // Validar tamaño del mensaje antes de enviar
                if (input.length() > Constants.MAX_MESSAGE_LENGTH) {
                    System.out.println("[!] Mensaje demasiado largo (max " +
                            Constants.MAX_MESSAGE_LENGTH + " caracteres)");
                    continue;
                }
                client.sendMessage(Constants.CMD_MSG + Constants.PROTOCOL_SEPARATOR +
                        recipient + Constants.PROTOCOL_SEPARATOR + input);
                System.out.println("[ENVIADO] Mensaje enviado a " + recipient);
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        return "Iniciar videollamada";
    }
}
