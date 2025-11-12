package com.mycompany.chat;

import java.io.*;
import java.util.Scanner;
import org.opencv.core.Core;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    public static void main(String[] args) {
        while (true) {
            mostrarMenu();
            String opcion = scanner.nextLine().trim();

            switch (opcion) {
                case "1":
                    configurarBaseDatos();
                    break;
                case "2":
                    iniciarServidor();
                    break;
                case "3":
                    iniciarCliente();
                    break;
                case "4":
                    verificarConexionDB();
                    break;
                case "5":
                    System.out.println("\n¡Hasta luego!");
                    System.exit(0);
                    break;
                case "6":
                    registrarUsuario();
                    break;
                default:
                    System.out.println("\nOpcion invalida. Intenta de nuevo.\n");
            }
        }
    }

private static void mostrarMenu() {
    System.out.println("======================================================");
    System.out.println("                   MENÚ PRINCIPAL                     ");
    System.out.println("======================================================");
    System.out.println("[1] Configurar base de datos MySQL");
    System.out.println("[2] Iniciar SERVIDOR");
    System.out.println("[3] Iniciar CLIENTE");
    System.out.println("[4] Verificar conexion a base de datos");
    System.out.println("[5] Salir");
    System.out.println("[6] Registrar nuevo usuario");
    System.out.println("\n-----------------------------------------------------");
    System.out.print("Selecciona una opción: ");
}


    private static void configurarBaseDatos() {
        System.out.println("\nConfiguracion de Base de Datos MySQL");
        System.out.println("\n-----------------------------------------------------");

        System.out.print("Usuario MySQL (default: root): ");
        String user = scanner.nextLine();
        if (user.isEmpty()) user = "root";

        System.out.print("Contrasena MySQL: ");
        String pass = scanner.nextLine();

        System.out.println("\nEjecutando script SQL...");
        
        EjecutorSql ejec = EjecutorSql.CreateEjecutorSql();
        ejec.CreateDatabase(user, pass);
        pausar();
    }

    private static void iniciarServidor() {
        System.out.println("\nIniciando Servidor del chat...");
        System.out.println("-----------------------------------------------------\n");
        System.out.print("Puerto del servidor (default 9000): ");
        String portStr = scanner.nextLine().trim();

        int port = 9000;
        if (!portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.out.println("Puerto inválido, usando 9000 por defecto.");
            }
        }

        System.out.println("\nEl servidor se ejecutara en esta ventana.");
        System.out.println("-----------------------------------------------------\n");
        pausar();

        ChatServer server = new ChatServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.start();
    }


    private static void iniciarCliente() {
        System.out.println("\nIniciando Cliente del chat...");
        System.out.println("-----------------------------------------------------\n");

        System.out.print("IP/host del servidor (default: localhost): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Puerto del servidor (default: 9000): ");
        String portStr = scanner.nextLine().trim();
        int port = 9000;
        if (!portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                System.out.println("Puerto inválido, usando 9000 por defecto.");
            }
        }

        System.out.println("\nEl cliente se ejecutara en esta ventana.");
        System.out.println("Para volver al menu, cierra el cliente con /logout");
        System.out.println("-----------------------------------------------------\n");

        pausar();

        ChatClient client = new ChatClient(host, port);
        client.connect();

        System.out.println("\nCliente cerrado. Volviendo al menu...");
    }


    private static void verificarConexionDB() {
        System.out.println("\nVerificando conexion a base de datos...");
        System.out.println("-----------------------------------------------------\n");

        boolean ok = Database.testConnection();

        while (!ok) {
            System.out.println("No se pudo conectar a la base de datos.\n");
            System.out.println("Opciones:");
            System.out.println("  [1] Reconfigurar credenciales de MySQL");
            System.out.println("  [2] Volver al menú principal");
            System.out.print("Elige una opcion: ");

            String op = scanner.nextLine().trim();
            if ("1".equals(op)) {
                configurarBaseDatos();
                ok = Database.testConnection();
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

    private static void pausar() {
        System.out.println("\nPresiona Enter para continuar...");
        scanner.nextLine();
    }
    private static void registrarUsuario() {
        System.out.println("\nRegistro de nuevo usuario");
        System.out.println("-----------------------------------------------------");

        System.out.print("Nuevo username: ");
        String user = scanner.nextLine().trim();

        System.out.print("Password: ");
        String pass = scanner.nextLine().trim();

        if (Database.registerUser(user, pass)) {
            System.out.println("Usuario registrado correctamente!");
        } else {
            System.out.println("No se pudo registrar el usuario. Revisa la consola para más detalles.");
        }

        pausar();
    }

}
