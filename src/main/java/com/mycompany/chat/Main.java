package com.mycompany.chat;

import java.util.Scanner;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    static {
        try {
            // Intentar cargar OpenCV nativo (opcional, solo necesario para video)
            String dllPath = System.getProperty("user.dir") + "/lib/native/opencv_java4120.dll";
            System.load(dllPath);
            System.out.println("OpenCV nativo cargado correctamente.");
        } catch (UnsatisfiedLinkError e) {
            // Si no se puede cargar, el chat seguirá funcionando sin video
            System.err.println("Advertencia: No se pudo cargar OpenCV nativo. El video no estará disponible.");
            System.err.println("  Ruta intentada: " + System.getProperty("user.dir") + "/lib/native/opencv_java4120.dll");
            System.err.println("  El chat funcionará normalmente, pero sin funcionalidad de video.\n");
        } catch (Exception e) {
            System.err.println("Advertencia: Error al cargar OpenCV: " + e.getMessage());
            System.err.println("  El chat funcionará normalmente, pero sin funcionalidad de video.\n");
        }
    }
    public static void main(String[] args) {
        try {
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
                        System.exit(0);
                        break;
                    default:
                        System.out.println("\nOpción invalida. Intentalo de nuevo.\n");
                }
            }
        } catch (Exception e) {
            System.err.println("\nError fatal en la aplicacion: " + e.getMessage());
            e.printStackTrace();
            System.err.println("\nPresiona Enter para salir...");
            try {
                scanner.nextLine();
            } catch (Exception ignored) {}
            System.exit(1);
        }
    }

    private static void mostrarMenuInicial() {
        System.out.println("======================================================");
        System.out.println("              MENÚ PRINCIPAL - SELECCIÓN               ");
        System.out.println("======================================================");
        System.out.println("[1] Modo SERVIDOR");
        System.out.println("[2] Modo CLIENTE");
        System.out.println("[3] Salir");
        System.out.println("\n-----------------------------------------------------");
        System.out.print("Selecciona una opción: ");
    }

    private static void menuServidor() {
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
                    System.out.println("\nOpción inválida. Intenta de nuevo.\n");
            }
        }
    }

    private static void mostrarMenuServidor() {
        System.out.println("\n======================================================");
        System.out.println("              MENÚ SERVIDOR                           ");
        System.out.println("======================================================");
        System.out.println("[1] Configurar base de datos MySQL");
        System.out.println("[2] Verificar conexión a base de datos");
        System.out.println("[3] Registrar nuevo usuario");
        System.out.println("[4] Iniciar servidor");
        System.out.println("[5] Volver al menú principal");
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
