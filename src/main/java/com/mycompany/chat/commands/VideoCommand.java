package com.mycompany.chat.commands;

import com.mycompany.chat.ChatClient;
import com.mycompany.chat.protocol.MessageBuilder;
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
            System.out.println("[!] Ya hay una videollamada activa.");
        } else {
            System.out.println("\nIniciando videollamada con " + recipient + "...");
            client.startVideoCall(recipient);
            if (!client.isVideoActive()) {
                System.out.println("[ERROR] No se pudo iniciar la videollamada.");
                return false;
            }
            System.out.println("[VIDEO] Videollamada activada.");
        }

        System.out.println("NOTA: El video se transmite en segundo plano.");
        System.out.println("      Puedes enviar mensajes y archivos mientras el video esta activo.");
        System.out.println("(Escribe 'volver' para regresar al menu o 'detener' para finalizar el video)\n");
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
            } else if (!input.isEmpty()) {
                System.out.println("[!] Comando no reconocido. Escribe 'volver' o 'detener'.\n");
            }

            // Si no está vacío y no es comando de detener, enviar como mensaje
            if (!input.isEmpty()) {
                // Validar tamaño del mensaje antes de enviar
                if (input.length() > Constants.MAX_MESSAGE_LENGTH) {
                    System.out.println("[!] Mensaje demasiado largo (max " +
                            Constants.MAX_MESSAGE_LENGTH + " caracteres)");
                    continue;
                }
                client.sendMessage(MessageBuilder.buildMessage(recipient, input));
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
