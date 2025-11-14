package com.mycompany.chat;

import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;


public class ChatClient {
    private String host;
    private int port;

    private Socket socket;
    private Socket videoSocket;
    private BufferedReader in;
    private PrintWriter out;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private DataOutputStream videoOut;
    private Scanner scanner;
    private volatile boolean running;
    private boolean videoActive;
    private JLabel videoLabel;
    private String currentRecipient; // Destinatario actual para mensajes
    private volatile boolean loginSuccessful = false; // Flag para sincronizar login
    
    private final Map<String, JLabel> videoViews = new ConcurrentHashMap<>();
    private final JFrame videoFrame = new JFrame("Videollamada");
    private final JPanel videoPanel = new JPanel(new GridLayout(0, 2, 5, 5));

    // Constructor por defecto => localhost:9000
    public ChatClient() {
        this("localhost", 9000);
        this.videoActive = false;
    }

    // Constructor configurable (lo usa Main)
    public ChatClient(String host, int port) {
        this.videoActive = false;
        this.host = host;
        this.port = port;
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
            System.out.println("Conectando al servidor " + host + ":" + port + "...\n");

            socket = new Socket(host, port);
            videoSocket = new Socket(host, port+1);
            videoOut = new DataOutputStream(videoSocket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());
            
            JFrame frame = new JFrame("Cliente de Video");
            videoLabel = new JLabel("Esperando video...");
            frame.add(new JScrollPane(videoPanel));
            frame.setSize(640, 480);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            System.out.println("Conectado al servidor!\n");

            // Hilo para recibir mensajes
            Thread receiveThread = new Thread(this::receiveMessages);
            
            // Hilo para leer mensajes del servidor
            new Thread(this::listenServer).start();
            receiveThread.setDaemon(true);
            receiveThread.start();

            // Esperar por el mensaje de bienvenida
            Thread.sleep(500);

            // Pedir credenciales
            System.out.println("-------------------------------------");
            System.out.println("           INICIAR SESION");
            System.out.println("-------------------------------------");
            System.out.print("Usuario: ");
            String username = scanner.nextLine().trim();

            System.out.print("Contrasena: ");
            String password = scanner.nextLine().trim();

            // Enviar login al servidor según nuevo formato
            sendMessage("LOGIN|" + username + "|" + password);

            // Esperar respuesta del login (máximo 5 segundos)
            int attempts = 0;
            while (!loginSuccessful && attempts < 50) {
                Thread.sleep(100);
                attempts++;
            }
            
            if (!loginSuccessful) {
                System.err.println("Error: No se recibió confirmación de login. Cerrando...");
                return;
            }
            
            System.out.println("-------------------------------------\n");
            
            // Ahora pedir que elija destinatario
            selectRecipientAndShowMenu();

        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            System.err.println("Asegúrate de que el servidor esté corriendo.");
        } catch (InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // Recibe mensajes del servidor (hilo separado)
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
        String[] parts = message.split("\\|", 4);
        String type = parts[0];

        switch (type) {
            case "SERVER":
                System.out.println("[SERVIDOR]: " + (parts.length > 1 ? parts[1] : ""));
                break;

            case "OK":
                if (parts.length >= 2 && "LOGIN".equals(parts[1])) {
                    System.out.println("\n" + (parts.length > 2 ? parts[2] : "Login exitoso"));
                    System.out.println("\n═══════════════════════════════════════════════════");
                    System.out.println("  ¡Bienvenido! Ahora debes seleccionar un destinatario");
                    System.out.println("═══════════════════════════════════════════════════\n");
                    loginSuccessful = true; // Marcar login como exitoso
                } else if (parts.length >= 2 && "MSG".equals(parts[1])) {
                    // Confirmación de mensaje enviado
                    if (parts.length > 2) {
                        System.out.println("✓ " + parts[2]);
                    }
                } else if (parts.length > 1) {
                    System.out.println("✓ " + parts[1]);
                }
                break;

            case "ERROR":
                System.out.println("Error: " + (parts.length > 1 ? parts[1] : "Error desconocido"));
                break;

            case "MSG":
                if (parts.length >= 3) {
                    String sender = parts[1];
                    String msg = parts[2];
                    // Mostrar mensaje privado recibido
                    System.out.println("[" + sender + "]: " + msg);
                }
                break;

            case "SYSTEM":
                if (parts.length >= 2) {
                    System.out.println(parts[1]);
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
                // Leer exactamente fileSize bytes del socket
                byte[] fileData = new byte[fileSize];
                int read = 0;
                while (read < fileSize) {
                    int r = dataIn.read(fileData, read, fileSize - read);
                    if (r == -1) throw new EOFException("Conexión cerrada durante la recepción del archivo");
                    read += r;
                }

                // Guardar archivo en /downloads
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

    // Selecciona destinatario y muestra el menú principal
    private void selectRecipientAndShowMenu() {
        while (running && (currentRecipient == null || currentRecipient.isEmpty())) {
            System.out.println("═══════════════════════════════════════════════════");
            System.out.println("           SELECCIONAR DESTINATARIO");
            System.out.println("═══════════════════════════════════════════════════\n");
            
            // Solicitar lista de usuarios
            sendMessage("USERS");
            
            // Esperar un momento para recibir la lista
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            System.out.print("\nIngresa el nombre del destinatario (o 'salir' para cerrar): ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("salir") || input.equalsIgnoreCase("exit")) {
                sendMessage("LOGOUT");
                running = false;
                return;
            }
            
            if (!input.isEmpty()) {
                currentRecipient = input;
                System.out.println("\n✓ Destinatario seleccionado: " + currentRecipient);
                System.out.println("═══════════════════════════════════════════════════\n");
                
                // Mostrar menú principal
                showMainMenu();
            } else {
                System.out.println("⚠️  Por favor ingresa un nombre de usuario válido.\n");
            }
        }
    }
    
    // Muestra el menú principal con opciones
    private void showMainMenu() {
        while (running && currentRecipient != null && !currentRecipient.isEmpty()) {
            System.out.println("\n═══════════════════════════════════════════════════");
            System.out.println("              MENÚ PRINCIPAL");
            System.out.println("═══════════════════════════════════════════════════");
            System.out.println("Destinatario actual: " + currentRecipient);
            System.out.println("───────────────────────────────────────────────────");
            System.out.println("[1] Enviar mensaje de chat");
            System.out.println("[2] Enviar archivo");
            System.out.println("[3] Iniciar videollamada");
            System.out.println("[4] Cambiar destinatario");
            System.out.println("[5] Salir");
            System.out.println("═══════════════════════════════════════════════════");
            System.out.print("Selecciona una opción: ");
            
            String option = scanner.nextLine().trim();
            
            switch (option) {
                case "1":
                    handleChatOption();
                    break;
                case "2":
                    handleFileOption();
                    break;
                case "3":
                    handleVideoOption();
                    break;
                case "4":
                    currentRecipient = null;
                    System.out.println("\nCambiando destinatario...\n");
                    selectRecipientAndShowMenu();
                    return;
                case "5":
                    sendMessage("LOGOUT");
                    running = false;
                    return;
                default:
                    System.out.println("\n⚠️  Opción inválida. Por favor selecciona 1-5.\n");
            }
        }
    }
    
    // Maneja la opción de chat
    private void handleChatOption() {
        System.out.println("\n───────────────────────────────────────────────────");
        System.out.println("           ENVIAR MENSAJE DE CHAT");
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("Destinatario: " + currentRecipient);
        System.out.println("(Escribe 'volver' para regresar al menú)\n");
        
        while (running && currentRecipient != null) {
            System.out.print("Mensaje: ");
            String message = scanner.nextLine().trim();
            
            if (message.equalsIgnoreCase("volver")) {
                break;
            }
            
            if (!message.isEmpty()) {
                sendMessage("MSG|" + currentRecipient + "|" + message);
            }
        }
    }
    
    // Maneja la opción de archivo
    private void handleFileOption() {
        System.out.println("\n───────────────────────────────────────────────────");
        System.out.println("              ENVIAR ARCHIVO");
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("Destinatario: " + currentRecipient);
        System.out.print("Ruta del archivo (o 'volver' para regresar): ");
        
        String filePath = scanner.nextLine().trim();
        
        if (filePath.equalsIgnoreCase("volver")) {
            return;
        }
        
        if (!filePath.isEmpty()) {
            sendFile(filePath);
        } else {
            System.out.println("⚠️  Ruta de archivo no válida.\n");
        }
    }
    
    // Maneja la opción de video
    private void handleVideoOption() {
        System.out.println("\n───────────────────────────────────────────────────");
        System.out.println("            VIDELLAMADA");
        System.out.println("───────────────────────────────────────────────────");
        System.out.println("Destinatario: " + currentRecipient);
        
        if (!videoActive) {
            System.out.println("\nIniciando videollamada con " + currentRecipient + "...");
            videoActive = true;
            // Enviar comando de video privado al servidor
            sendMessage("VIDEO|START|" + currentRecipient);
            new Thread(this::sendVideo).start();
            new Thread(this::receiveVideo).start();
            System.out.println("📹 Videollamada activada. Escribe 'detener' para finalizar.\n");
            
            // Esperar comando para detener
            while (videoActive && running) {
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("detener") || input.equalsIgnoreCase("stop")) {
                    videoActive = false;
                    sendMessage("VIDEO|STOP");
                    System.out.println("📴 Videollamada detenida.\n");
                    break;
                }
            }
        } else {
            System.out.println("⚠️  Ya hay una videollamada activa. Detén la actual primero.\n");
        }
    }

    // Maneja la entrada del usuario (método antiguo, ahora no se usa directamente)
    private void handleUserInput() {
        while (running) {
            try {
                String input = scanner.nextLine();

                if (input == null || input.trim().isEmpty()) {
                    continue;
                }

                if (input.startsWith("/")) {
                    handleCommand(input);
                } else {
                    // Enviar mensaje privado al destinatario actual
                    if (currentRecipient == null || currentRecipient.isEmpty()) {
                        System.out.println("⚠️  Error: No has seleccionado un destinatario.");
                        System.out.println("   Usa /chat <usuario> para seleccionar un destinatario primero.");
                        System.out.println("   Usa /users para ver los usuarios disponibles.\n");
                    } else {
                        sendMessage("MSG|" + currentRecipient + "|" + input);
                    }
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
            case "/chat":
                if (parts.length < 2) {
                    System.out.println("Uso: /chat <usuario>");
                    System.out.println("Ejemplo: /chat juan");
                    if (currentRecipient != null) {
                        System.out.println("Destinatario actual: " + currentRecipient);
                    }
                } else {
                    String newRecipient = parts[1].trim();
                    currentRecipient = newRecipient;
                    System.out.println("✓ Destinatario seleccionado: " + currentRecipient);
                    System.out.println("  Ahora puedes escribir mensajes que solo verá " + currentRecipient + "\n");
                }
                break;
                
            case "/users":
                sendMessage("USERS");
                break;
                
            case "/logout":
                sendMessage("LOGOUT");
                running = false;
                break;

            case "/file":
                if (currentRecipient == null || currentRecipient.isEmpty()) {
                    System.out.println("⚠️  Error: No has seleccionado un destinatario.");
                    System.out.println("   Usa /chat <usuario> para seleccionar un destinatario primero.");
                } else if (parts.length < 2) {
                    System.out.println("Uso: /file <ruta_del_archivo>");
                    System.out.println("El archivo se enviará a: " + currentRecipient);
                } else {
                    sendFile(parts[1]);
                }
                break;

            case "/help":
                showHelp();
                break;
                
            case "/video":
                // El video ahora se maneja desde el menú principal
                System.out.println("⚠️  Usa el menú principal (opción 3) para iniciar videollamada.");
                break;

            default:
                System.out.println("Comando desconocido: " + command);
                System.out.println("Escribe /help para ver los comandos disponibles");
        }
    }

    // Envía un archivo al servidor (al destinatario actual)
    private void sendFile(String filePath) {
        if (currentRecipient == null || currentRecipient.isEmpty()) {
            System.out.println("⚠️  Error: No has seleccionado un destinatario.");
            return;
        }
        
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
            int fileSize = fileData.length;

            if (fileData.length > 50 * 1024 * 1024) { // 50MB límite
                System.out.println("Archivo demasiado grande (max 50MB)");
                return;
            }

            // Avisar al servidor que viene un archivo (formato: FILE|destinatario|nombre|tamaño)
            sendMessage("FILE|" + currentRecipient + "|" + fileName + "|" + fileSize);
            try {
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
            }

            // Enviar tamaño y datos
            dataOut.write(fileData);
            dataOut.flush();

            System.out.println("Enviando archivo a " + currentRecipient + ": " + fileName + " (" + fileSize + " bytes)");

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
        System.out.println("  /chat <usuario>");
        System.out.println("    Selecciona el destinatario para tus mensajes");
        System.out.println("    Ejemplo: /chat juan");
        System.out.println();
        System.out.println("  /users");
        System.out.println("    Muestra la lista de usuarios conectados");
        System.out.println();
        System.out.println("  Mensaje normal:");
        System.out.println("    Escribe tu mensaje y presiona Enter");
        System.out.println("    (Solo funciona si has seleccionado un destinatario)");
        System.out.println();
        System.out.println("  /file <ruta>");
        System.out.println("    Envía un archivo al destinatario actual");
        System.out.println();
        System.out.println("  /video");
        System.out.println("    Activa/desactiva la videollamada");
        System.out.println();
        System.out.println("  /logout");
        System.out.println("    Cierra la sesión y sale del chat");
        System.out.println();
        System.out.println("  /help");
        System.out.println("    Muestra esta ayuda");
        System.out.println("─────────────────────────────────────────\n");
        if (currentRecipient != null) {
            System.out.println("Destinatario actual: " + currentRecipient + "\n");
        }
    }

    // Desconecta del servidor
    private void disconnect() {
        running = false;

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (dataIn != null) dataIn.close();
            if (dataOut != null) dataOut.close();
            if (socket != null && !socket.isClosed()) socket.close();
            // No cerramos scanner para no matar System.in
        } catch (IOException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }

        System.out.println("\nDesconectado del servidor. ¡Hasta luego!");
    }
    private void sendVideo() {
        VideoCapture cam = new VideoCapture(0);
        if (!cam.isOpened()) {
            System.out.println("No se pudo abrir la cámara.");
            return;
        }

        Mat frame = new Mat();
        try {
            while (videoActive && cam.read(frame)) {
                BytePointer mob = new BytePointer();
                imencode(".jpg", frame, mob);
                byte[] bytes = new byte[(int) mob.limit()];
                mob.get(bytes);

                // Enviar el frame al servidor
                videoOut.writeInt(bytes.length);
                videoOut.write(bytes);
                videoOut.flush();

                // Mostrar el frame localmente 👇
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) {
                    SwingUtilities.invokeLater(() -> {
                        JLabel label = videoViews.computeIfAbsent("Yo", s -> {
                            JLabel l = new JLabel();
                            videoPanel.add(l);
                            videoPanel.revalidate();
                            videoPanel.repaint();
                            return l;
                        });
                        label.setIcon(new ImageIcon(img));
                    });
                }

                Thread.sleep(50); // 20 FPS aprox
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cam.release();
        }
    }
    private void receiveVideo() {
        try (DataInputStream videoIn = new DataInputStream(videoSocket.getInputStream())) {
            while (videoActive) {
                int nameLen = videoIn.readInt();
                byte[] nameBytes = new byte[nameLen];
                videoIn.readFully(nameBytes);
                String sender = new String(nameBytes);

                int frameLen = videoIn.readInt();
                byte[] frame = new byte[frameLen];
                videoIn.readFully(frame);

                BufferedImage img = ImageIO.read(new ByteArrayInputStream(frame));
                if (img != null) {
                    SwingUtilities.invokeLater(() -> {
                        JLabel label = videoViews.computeIfAbsent(sender, s -> {
                            JLabel l = new JLabel("Cargando...");
                            videoPanel.add(l);
                            videoPanel.revalidate();
                            videoPanel.repaint();
                            return l;
                        });
                        label.setIcon(new ImageIcon(img));
                    });
                }
            }
        } catch (IOException e) {
            System.out.println("Error al recibir video: " + e.getMessage());
        }
    }

    private void listenServer() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                String command = parts[0];

                switch (command) {
                    case "SERVER":
                        System.out.println("[Servidor]: " + parts[1]);
                        break;

                    case "MSG":
                        System.out.println(parts[1]);
                        break;

                    case "VIDEO":
                        // Si el servidor manda "VIDEO|FRAME", recibimos los bytes
                        if (parts.length > 1 && parts[1].equals("FRAME")) {
                            receiveVideo();
                        }
                        break;

                    default:
                        System.out.println("[Desconocido]: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error escuchando servidor: " + e.getMessage());
        }
    }
    private void startVideoCapture() {
        new Thread(() -> {
            try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0)) {
                grabber.start();
                videoActive = true;
                while (videoActive) {
                    org.bytedeco.javacv.Frame frame = grabber.grab();
                    if (frame == null) continue;
                    BufferedImage image = new Java2DFrameConverter().convert(frame);
                    if (image == null) continue;

                    // Convertir frame a bytes (JPEG)
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    byte[] frameBytes = baos.toByteArray();

                    // Enviar al servidor
                    synchronized (dataOut) {
                        dataOut.writeInt(frameBytes.length);
                        dataOut.write(frameBytes);
                        dataOut.flush();
                    }

                    // Mostrar video localmente
                    SwingUtilities.invokeLater(() -> {
                        videoLabel.setIcon(new ImageIcon(image));
                    });

                    Thread.sleep(33); // ~30 FPS
                }
                grabber.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        client.connect();
    }
}
