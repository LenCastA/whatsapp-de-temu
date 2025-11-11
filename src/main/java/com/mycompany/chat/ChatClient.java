package com.mycompany.chat;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_HOST = "localhost"; // Cambiar a IP del servidor
    private static final int SERVER_PORT = 9000;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private Scanner scanner;
    private volatile boolean running;

    public ChatClient() {
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    /**
     * Conecta al servidor y inicia la sesión
     */
    public void connect() {
        try {
            System.out.println("===========================================");
            System.out.println("        Cliente de Chat - Terminal        ");
            System.out.println("===========================================");
            System.out.println("Conectando al servidor " + SERVER_HOST + ":" + SERVER_PORT + "...\n");

            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());

            System.out.println("Conectado al servidor!\n");

            // Iniciar hilo para recibir mensajes
            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();

            // Esperar mensaje de bienvenida del servidor
            Thread.sleep(500);

            // Pedir credenciales de login
            System.out.println("-------------------------------------");
            System.out.println("           INICIAR SESION");
            System.out.println("-------------------------------------");
            System.out.print("Usuario: ");
            String username = scanner.nextLine().trim();

            System.out.print("Contrasena: ");
            String password = scanner.nextLine().trim();

            // CAMBIAR por el nuevo formato /login
            sendMessage("LOGIN|" + username + "|" + password);

            // Esperar respuesta del servidor
            Thread.sleep(1000);
            
            System.out.println("-------------------------------------\n");

            // Manejar entrada del usuario
            handleUserInput();

        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            System.err.println("Asegúrate de que el servidor esté corriendo.");
        } catch (InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // Recibe mensajes del servidor (corre en hilo separado)
    private void receiveMessages() {
        try {
            String message;
            while (running && (message = in.readLine()) != null) {
                processIncomingMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("\nConexion perdida con el servidor");
                running = false;
            }
        }
    }

    // Procesa mensajes entrantes del servidor
    private void processIncomingMessage(String message) {
        String[] parts = message.split("\\|", 3);
        String type = parts[0];

        switch (type) {
            case "SERVER":
                System.out.println("[SERVIDOR]: " + parts[1]);
                break;

            case "OK":
                if (parts.length >= 2 && "LOGIN".equals(parts[1])) {
                    System.out.println("\n" + (parts.length > 2 ? parts[2] : "Login exitoso"));
                    System.out.println("\nComandos disponibles:");
                    System.out.println("  - Escribe tu mensaje y presiona Enter");
                    System.out.println("  - /file <ruta> - Enviar archivo");
                    System.out.println("  - /logout - Cerrar sesion");
                    System.out.println("  - /help - Mostrar ayuda");
                    System.out.println("─────────────────────────────────────────\n");
                } else {
                    System.out.println("" + (parts.length > 1 ? parts[1] : "OK"));
                }
                break;

            case "ERROR":
                System.out.println("Error: " + (parts.length > 1 ? parts[1] : "Error desconocido"));
                break;

            case "MSG":
                if (parts.length >= 3) {
                    String sender = parts[1];
                    String msg = parts[2];
                    System.out.println("[" + sender + "]: " + msg);
                }
                break;

            case "SYSTEM":
                if (parts.length >= 2) {
                    System.out.println("" + parts[1]);
                }
                break;

            case "FILE":
                if (parts.length >= 3) {
                    String fileName = parts[1];
                    int fileSize = Integer.parseInt(parts[2]);
                    receiveFile(fileName, fileSize);
                }
                break;

            default:
                System.out.println(message);
        }
    }

    // Recibe un archivo del servidor
    private void receiveFile(String fileName, int fileSize) {
        try {
            // Leer tamaño
            int size = dataIn.readInt();

            // Leer datos
            byte[] fileData = new byte[size];
            dataIn.readFully(fileData);

            // Guardar archivo en directorio downloads
            Path downloadDir = Paths.get("downloads");
            if (!Files.exists(downloadDir)) {
                Files.createDirectories(downloadDir);
            }

            Path filePath = downloadDir.resolve(fileName);
            Files.write(filePath, fileData);

            System.out.println("Archivo recibido: " + fileName + " (" + fileSize + " bytes)");
            System.out.println("   Guardado en: " + filePath.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error recibiendo archivo: " + e.getMessage());
        }
    }

    // Maneja la entrada del usuario
    private void handleUserInput() {
        while (running) {
            try {
                String input = scanner.nextLine();

                if (input == null || input.trim().isEmpty()) {
                    continue;
                }

                // Comandos especiales
                if (input.startsWith("/")) {
                    handleCommand(input);
                } else {
                    // Mensaje normal
                    sendMessage("MSG|" + input);
                }

            } catch (Exception e) {
                if (running) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
    }

    // Maneja comandos especiales del cliente
    private void handleCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/logout":
                sendMessage("LOGOUT");
                running = false;
                break;

            case "/file":
                if (parts.length < 2) {
                    System.out.println("Uso: /file <ruta_del_archivo>");
                } else {
                    sendFile(parts[1]);
                }
                break;

            case "/help":
                showHelp();
                break;

            default:
                System.out.println("Comando desconocido: " + command);
                System.out.println("Escribe /help para ver los comandos disponibles");
        }
    }

    // Envía un archivo al servidor
    private void sendFile(String filePath) {
        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                System.out.println("Archivo no encontrado: " + filePath);
                return;
            }

            if (!Files.isRegularFile(path)) {
                System.out.println("No es un archivo valido: " + filePath);
                return;
            }

            byte[] fileData = Files.readAllBytes(path);
            String fileName = path.getFileName().toString();

            if (fileData.length > 50 * 1024 * 1024) { // 50MB límite
                System.out.println("Archivo demasiado grande (max 50MB)");
                return;
            }

            // Enviar comando FILE
            sendMessage("FILE|" + fileName);

            // Enviar tamaño y datos
            dataOut.writeInt(fileData.length);
            dataOut.write(fileData);
            dataOut.flush();

            System.out.println("Enviando archivo: " + fileName + " (" + fileData.length + " bytes)");

        } catch (IOException e) {
            System.err.println("Error enviando archivo: " + e.getMessage());
        }
    }

    // Envía un mensaje al servidor
    private void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    private void showHelp() {
        System.out.println("\n─────────────────────────────────────────");
        System.out.println("COMANDOS DISPONIBLES:");
        System.out.println("─────────────────────────────────────────");
        System.out.println("  Mensaje normal:");
        System.out.println("    Simplemente escribe tu mensaje y presiona Enter");
        System.out.println();
        System.out.println("  /file <ruta>");
        System.out.println("    Envía un archivo a todos los usuarios");
        System.out.println("    Ejemplo: /file C:\\Users\\usuario\\imagen.jpg");
        System.out.println();
        System.out.println("  /logout");
        System.out.println("    Cierra la sesión y sale del chat");
        System.out.println();
        System.out.println("  /help");
        System.out.println("    Muestra esta ayuda");
        System.out.println("─────────────────────────────────────────\n");
    }

    /**
     * Desconecta del servidor
     */
    private void disconnect() {
        running = false;

        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (dataIn != null)
                dataIn.close();
            if (dataOut != null)
                dataOut.close();
            if (socket != null && !socket.isClosed())
                socket.close();
            // NO cerrar scanner porque cierra System.in y causa problemas en Main.java
            // if (scanner != null)
            // scanner.close();
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }

        System.out.println("\nDesconectado del servidor. ¡Hasta luego!");
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();

        // Agregar shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));

        client.connect();
    }
}
