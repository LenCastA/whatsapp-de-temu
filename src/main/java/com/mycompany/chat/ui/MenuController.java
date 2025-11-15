package com.mycompany.chat.ui;

import com.mycompany.chat.ChatClient;
import com.mycompany.chat.ChatServer;
import com.mycompany.chat.security.InputValidator;
import com.mycompany.chat.service.ClientService;
import com.mycompany.chat.service.DatabaseService;
import com.mycompany.chat.service.ServerService;
import java.util.Scanner;

/**
 * Controlador de menús que maneja la interfaz de usuario.
 * Separa la presentación de la lógica de negocio.
 */
public class MenuController {
    private final Scanner scanner;
    private final DatabaseService databaseService;
    private final ServerService serverService;
    private final ClientService clientService;
    
    public MenuController(Scanner scanner) {
        this.scanner = scanner;
        this.databaseService = new DatabaseService();
        this.serverService = new ServerService();
        this.clientService = new ClientService();
    }
    
    /**
     * Muestra el menú principal y maneja la selección del usuario.
     */
    public void mostrarMenuPrincipal() {
        while (true) {
            mostrarMenuInicial();
            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    menuServidor();
                    break;
                case "2":
                    iniciarCliente();
                    break;
                case "3":
                    System.out.println("\nSaliendo del programa...");
                    return;
                default:
                    System.out.println("\nOpcion invalida. Intentalo de nuevo.\n");
            }
        }
    }
    
    private void mostrarMenuInicial() {
        System.out.println("======================================================");
        System.out.println("              MENU PRINCIPAL - SELECCION               ");
        System.out.println("======================================================");
        System.out.println("[1] Modo SERVIDOR");
        System.out.println("[2] Modo CLIENTE");
        System.out.println("[3] Salir");
        System.out.println("\n-----------------------------------------------------");
        System.out.print("Selecciona una opcion: ");
    }
    
    private void menuServidor() {
        while (true) {
            mostrarMenuServidor();
            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    configurarBaseDatos();
                    break;
                case "2":
                    verificarConexionDB();
                    break;
                case "3":
                    registrarUsuario();
                    break;
                case "4":
                    iniciarServidor();
                    return;
                case "5":
                    return; // Volver al menú principal
                default:
                    System.out.println("\nOpcion invalida. Intenta de nuevo.\n");
            }
        }
    }
    
    private void mostrarMenuServidor() {
        System.out.println("\n======================================================");
        System.out.println("              MENU SERVIDOR                           ");
        System.out.println("======================================================");
        System.out.println("[1] Configurar base de datos MySQL");
        System.out.println("[2] Verificar conexion a base de datos");
        System.out.println("[3] Registrar nuevo usuario");
        System.out.println("[4] Iniciar servidor");
        System.out.println("[5] Volver al menu principal");
        System.out.println("\n-----------------------------------------------------");
        System.out.print("Selecciona una opcion: ");
    }
    
    private void configurarBaseDatos() {
        System.out.println("\nConfiguracion de Base de Datos MySQL");
        System.out.println("\n-----------------------------------------------------");

        System.out.print("Usuario MySQL (default: root): ");
        String user = scanner.nextLine();

        System.out.print("Contrasena MySQL: ");
        String pass = scanner.nextLine();

        System.out.println("\nEjecutando script SQL...");
        
        if (databaseService.configurarBaseDatos(user, pass)) {
            System.out.println("Base de datos configurada exitosamente.");
        } else {
            System.out.println("Error al configurar la base de datos.");
        }
        
        pausar();
    }
    
    private void iniciarServidor() {
        System.out.println("\nIniciando Servidor del chat...");
        System.out.println("-----------------------------------------------------\n");
        System.out.print("Puerto del servidor (default 9000): ");
        String portStr = scanner.nextLine().trim();

        System.out.println("\nEl servidor se ejecutara en esta ventana.");
        System.out.println("-----------------------------------------------------\n");
        pausar();

        ChatServer server = serverService.iniciarServidor(portStr);
        if (server != null) {
            server.start();
        } else {
            System.err.println("No se pudo iniciar el servidor.");
        }
    }
    
    private void iniciarCliente() {
        System.out.println("\nIniciando Cliente del chat...");
        System.out.println("-----------------------------------------------------\n");

        String host;
        while (true) {
            System.out.print("IP/host del servidor (default: localhost): ");
            String hostInput = scanner.nextLine().trim();
            if (hostInput.isEmpty()) {
                host = "localhost";
                break;
            }
            String error = InputValidator.validateHost(hostInput);
            if (error == null) {
                host = hostInput;
                break;
            } else {
                System.out.println("Error: " + error);
            }
        }

        System.out.print("Puerto del servidor (default: 9000): ");
        String portStr = scanner.nextLine().trim();

        System.out.println("\nEl cliente se ejecutara en esta ventana.");
        System.out.println("Para volver al menu, cierra el cliente con /logout");
        System.out.println("-----------------------------------------------------\n");

        pausar();

        ChatClient client = clientService.iniciarCliente(host, portStr);
        if (client != null) {
            client.connect();
            System.out.println("\nCliente cerrado. Volviendo al menu...");
        } else {
            System.err.println("No se pudo iniciar el cliente.");
        }
    }
    
    private void verificarConexionDB() {
        System.out.println("\nVerificando conexion a base de datos...");
        System.out.println("-----------------------------------------------------\n");

        boolean ok = databaseService.verificarConexion();

        while (!ok) {
            System.out.println("No se pudo conectar a la base de datos.\n");
            System.out.println("Opciones:");
            System.out.println("  [1] Reconfigurar credenciales de MySQL");
            System.out.println("  [2] Volver al menu principal");
            System.out.print("Elige una opcion: ");

            String op = scanner.nextLine().trim();
            if ("1".equals(op)) {
                configurarBaseDatos();
                ok = databaseService.verificarConexion();
            } else if ("2".equals(op)) {
                return;
            } else {
                System.out.println("Opcion invalida.\n");
            }
        }

        System.out.println("Conexion exitosa a la base de datos MySQL!");
        System.out.println("    Base de datos: chatdb");
        System.out.println("    Estado: Listo para usar");

        pausar();
    }
    
    private void registrarUsuario() {
        System.out.println("\nRegistro de nuevo usuario");
        System.out.println("-----------------------------------------------------");

        String user;
        while (true) {
            System.out.print("Nuevo username: ");
            String userInput = scanner.nextLine().trim();
            String error = InputValidator.validateUsername(userInput);
            if (error == null) {
                user = userInput;
                break;
            } else {
                System.out.println("Error: " + error);
            }
        }

        String pass;
        while (true) {
            System.out.print("Password: ");
            String passInput = scanner.nextLine().trim();
            String error = InputValidator.validatePassword(passInput);
            if (error == null) {
                pass = passInput;
                break;
            } else {
                System.out.println("Error: " + error);
            }
        }

        if (databaseService.registrarUsuario(user, pass)) {
            System.out.println("Usuario registrado correctamente!");
        } else {
            System.out.println("No se pudo registrar el usuario. Revisa la consola para mas detalles.");
        }

        pausar();
    }
    
    private void pausar() {
        System.out.println("\nPresiona Enter para continuar...");
        scanner.nextLine();
    }
}

