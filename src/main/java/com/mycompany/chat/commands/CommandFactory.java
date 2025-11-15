package com.mycompany.chat.commands;

import com.mycompany.chat.ChatClient;
import java.util.Scanner;

/**
 * Factory para crear instancias de comandos.
 * Implementa el patrón Factory Method para centralizar la creación de objetos Command.
 * 
 * Este patrón permite:
 * - Centralizar la lógica de creación de comandos
 * - Facilitar la extensión con nuevos tipos de comandos
 * - Reducir el acoplamiento entre el cliente y las clases concretas de Command
 */
public class CommandFactory {
    private final ChatClient client;
    private final Scanner scanner;
    
    /**
     * Constructor del Factory.
     * 
     * @param client instancia de ChatClient necesaria para los comandos
     * @param scanner instancia de Scanner para entrada del usuario
     */
    public CommandFactory(ChatClient client, Scanner scanner) {
        this.client = client;
        this.scanner = scanner;
    }
    
    /**
     * Crea una instancia de ChatCommand.
     * 
     * @return nueva instancia de ChatCommand
     */
    public Command createChatCommand() {
        return new ChatCommand(client, scanner);
    }
    
    /**
     * Crea una instancia de FileCommand.
     * 
     * @return nueva instancia de FileCommand
     */
    public Command createFileCommand() {
        return new FileCommand(client, scanner);
    }
    
    /**
     * Crea una instancia de VideoCommand.
     * 
     * @return nueva instancia de VideoCommand
     */
    public Command createVideoCommand() {
        return new VideoCommand(client, scanner);
    }
    
    /**
     * Crea una instancia de ChangeRecipientCommand.
     * 
     * @return nueva instancia de ChangeRecipientCommand
     */
    public Command createChangeRecipientCommand() {
        return new ChangeRecipientCommand(client);
    }
    
    /**
     * Crea una instancia de ExitCommand.
     * 
     * @return nueva instancia de ExitCommand
     */
    public Command createExitCommand() {
        return new ExitCommand(client);
    }
    
    /**
     * Crea un comando basado en su clave.
     * Este método proporciona una forma alternativa de crear comandos.
     * 
     * @param commandKey clave del comando ("1", "2", "3", "4", "5")
     * @return instancia del comando correspondiente, o null si la clave no es válida
     */
    public Command createCommand(String commandKey) {
        switch (commandKey) {
            case "1":
                return createChatCommand();
            case "2":
                return createFileCommand();
            case "3":
                return createVideoCommand();
            case "4":
                return createChangeRecipientCommand();
            case "5":
                return createExitCommand();
            default:
                return null;
        }
    }
}

