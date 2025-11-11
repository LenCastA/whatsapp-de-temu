package com.mycompany.chat;

import java.io.*;
import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

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
        
        // Implementar la ejecución de schema.sql. Intentar con ProcessBuilder (se usa para comandos en la shell)
        String sqlFile = "src/main/resources/schema.sql";
        pausar();
    }

    private static void iniciarServidor() {
        System.out.println("\nIniciando Servidor del chat...");
        System.out.println("-----------------------------------------------------\n");
        System.out.println("El servidor se ejecutara en esta ventana.");
        System.out.println("-----------------------------------------------------\n");

        pausar();

        // Inicio del servidor
        ChatServer server = new ChatServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.start();
    }

    private static void iniciarCliente() {
        System.out.println("\nIniciando Cliente del chat...");
        System.out.println("-----------------------------------------------------\n");
        System.out.println("El cliente se ejecutara en esta ventana.");
        System.out.println("Para volver al menu, cierra el cliente con /logout");
        System.out.println("-----------------------------------------------------\n");

        pausar();

        // Iniciar el cliente
        ChatClient client = new ChatClient();
        client.connect();

        // Volviendo al menú cuando el cliente termina
        System.out.println("\nCliente cerrado. Volviendo al menu...");
    }

    private static void verificarConexionDB() {
        System.out.println("\nVerificando conexion a base de datos...");
        System.out.println("-----------------------------------------------------\n");

        try {
            if (Database.testConnection()) {
                System.out.println("Conexion exitosa a la base de datos MySQL!");
                System.out.println("    Base de datos: chatdb");
                System.out.println("    Estado: Listo para usar");
            } else {
                System.out.println("No se pudo conectar a la base de datos.");
                System.out.println("\nVerifica:");
                System.out.println("    MySQL esta corriendo");
                System.out.println("    Ejecutaste la opcion [1] para configurar DB");
                System.out.println("    Las credenciales en Database.java son correctas");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        pausar();
    }

    private static void pausar() {
        System.out.println("\nPresiona Enter para continuar...");
        scanner.nextLine();
    }
}
