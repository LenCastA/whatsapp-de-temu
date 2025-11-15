package com.mycompany.chat;

import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

import com.mycompany.chat.commands.ChangeRecipientCommand;
import com.mycompany.chat.commands.ChatCommand;
import com.mycompany.chat.commands.Command;
import com.mycompany.chat.commands.ExitCommand;
import com.mycompany.chat.commands.FileCommand;
import com.mycompany.chat.commands.MenuCommandInvoker;
import com.mycompany.chat.commands.VideoCommand;

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
    private String currentRecipient; // Destinatario actual para mensajes
    private volatile boolean loginSuccessful = false; // Flag para sincronizar login
    
    private final Map<String, JLabel> videoViews = new ConcurrentHashMap<>();
    private JFrame videoFrame; // Ventana de video (se crea solo cuando se inicia video)
    private JPanel videoPanel; // Panel de video (se crea solo cuando se inicia video)
    private ExecutorService executorService; // Pool de threads para gestionar hilos

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
        this.executorService = Executors.newCachedThreadPool();
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

            System.out.println("Conectado al servidor!\n");

            // Hilo para recibir mensajes del servidor (gestionado por ExecutorService)
            executorService.submit(this::receiveMessages);

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
            System.err.println("Error de conexion: " + e.getMessage());
            System.err.println("Asegurate de que el servidor este corriendo.");
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
                    System.out.println("\n====================================================");
                    System.out.println("  !Bienvenido! Ahora debes seleccionar un destinatario");
                    System.out.println("====================================================\n");
                    loginSuccessful = true; // Marcar login como exitoso
                } else if (parts.length >= 2 && "MSG".equals(parts[1])) {
                    // Confirmación de mensaje enviado
                    if (parts.length > 2) {
                        System.out.println("[OK] " + parts[2]);
                    }
                } else if (parts.length > 1) {
                    System.out.println("[OK] " + parts[1]);
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
                    if (r == -1) throw new EOFException("Conexion cerrada durante la recepcion del archivo");
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
            System.out.println("====================================================");
            System.out.println("           SELECCIONAR DESTINATARIO");
            System.out.println("====================================================\n");
            
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
                System.out.println("\n[OK] Destinatario seleccionado: " + currentRecipient);
                System.out.println("====================================================\n");
                
                // Mostrar menú principal
                showMainMenu();
            } else {
                System.out.println("[!] Por favor ingresa un nombre de usuario valido.\n");
            }
        }
    }
    
    // Muestra el menú principal con opciones usando el patrón Command
    private void showMainMenu() {
        // Inicializar el invocador de comandos y registrar todos los comandos
        MenuCommandInvoker invoker = new MenuCommandInvoker();
        invoker.registerCommand("1", new ChatCommand(this, scanner));
        invoker.registerCommand("2", new FileCommand(this, scanner));
        invoker.registerCommand("3", new VideoCommand(this, scanner));
        invoker.registerCommand("4", new ChangeRecipientCommand(this));
        invoker.registerCommand("5", new ExitCommand(this));
        
        while (running && currentRecipient != null && !currentRecipient.isEmpty()) {
            System.out.println("\n====================================================");
            System.out.println("              MENU PRINCIPAL");
            System.out.println("====================================================");
            System.out.println("Destinatario actual: " + currentRecipient);
            System.out.println("----------------------------------------------------");
            
            // Mostrar opciones dinámicamente desde los comandos registrados
            for (Map.Entry<String, Command> entry : invoker.getCommands().entrySet()) {
                System.out.println("[" + entry.getKey() + "] " + entry.getValue().getDescription());
            }
            
            System.out.println("====================================================");
            System.out.print("Selecciona una opcion: ");
            
            String option = scanner.nextLine().trim();
            
            // Ejecutar comando usando el invocador
            boolean executed = invoker.executeCommand(option);
            
            if (!executed) {
                System.out.println("\n[!] Opcion invalida. Por favor selecciona una opcion valida.\n");
            } else if ("4".equals(option)) {
                // Si se cambió el destinatario, volver a seleccionar
                selectRecipientAndShowMenu();
                return;
            } else if ("5".equals(option)) {
                // Si se salió, terminar el bucle
                return;
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
                    System.out.println("[OK] Destinatario seleccionado: " + currentRecipient);
                    System.out.println("  Ahora puedes escribir mensajes que solo vera " + currentRecipient + "\n");
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
                    System.out.println("[!] Error: No has seleccionado un destinatario.");
                    System.out.println("   Usa /chat <usuario> para seleccionar un destinatario primero.");
                } else if (parts.length < 2) {
                    System.out.println("Uso: /file <ruta_del_archivo>");
                    System.out.println("El archivo se enviara a: " + currentRecipient);
                } else {
                    sendFile(parts[1]);
                }
                break;

            case "/help":
                showHelp();
                break;
                
            case "/video":
                // El video ahora se maneja desde el menú principal
                System.out.println("[!] Usa el menu principal (opcion 3) para iniciar videollamada.");
                break;

            default:
                System.out.println("Comando desconocido: " + command);
                System.out.println("Escribe /help para ver los comandos disponibles");
        }
    }

    // Métodos públicos para que los comandos puedan acceder
    public String getCurrentRecipient() {
        return currentRecipient;
    }
    
    public void setCurrentRecipient(String recipient) {
        this.currentRecipient = recipient;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void setRunning(boolean running) {
        this.running = running;
    }
    
    public boolean isVideoActive() {
        return videoActive;
    }
    
    public void startVideoCall(String recipient) {
        videoActive = true;
        
        // Crear ventana de video solo cuando se inicia la videollamada
        if (videoFrame == null) {
            videoPanel = new JPanel(new GridLayout(0, 2, 5, 5));
            videoFrame = new JFrame("Videollamada");
            videoFrame.add(new JScrollPane(videoPanel));
            videoFrame.setSize(640, 480);
            videoFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            videoFrame.setVisible(true);
        }
        
        sendMessage("VIDEO|START|" + recipient);
        executorService.submit(this::sendVideo);
        executorService.submit(this::receiveVideo);
    }
    
    public void stopVideoCall() {
        videoActive = false;
        sendMessage("VIDEO|STOP");
        
        // Cerrar y limpiar la ventana de video
        if (videoFrame != null) {
            SwingUtilities.invokeLater(() -> {
                videoFrame.setVisible(false);
                videoFrame.dispose();
                videoFrame = null;
                videoPanel = null;
                videoViews.clear();
            });
        }
    }
    
    // Envía un archivo al servidor (al destinatario actual)
    public void sendFile(String filePath) {
        if (currentRecipient == null || currentRecipient.isEmpty()) {
            System.out.println("[!] Error: No has seleccionado un destinatario.");
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
    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    private void showHelp() {
        System.out.println("\n----------------------------------------------------");
        System.out.println("COMANDOS DISPONIBLES:");
        System.out.println("----------------------------------------------------");
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
        System.out.println("    Envia un archivo al destinatario actual");
        System.out.println();
        System.out.println("  /video");
        System.out.println("    Activa/desactiva la videollamada");
        System.out.println();
        System.out.println("  /logout");
        System.out.println("    Cierra la sesion y sale del chat");
        System.out.println();
        System.out.println("  /help");
        System.out.println("    Muestra esta ayuda");
        System.out.println("----------------------------------------------------\n");
        if (currentRecipient != null) {
            System.out.println("Destinatario actual: " + currentRecipient + "\n");
        }
    }

    // Desconecta del servidor
    private void disconnect() {
        running = false;
        videoActive = false; // Detener video si está activo

        // Cerrar ventana de video si está abierta
        if (videoFrame != null) {
            SwingUtilities.invokeLater(() -> {
                if (videoFrame != null) {
                    videoFrame.setVisible(false);
                    videoFrame.dispose();
                    videoFrame = null;
                    videoPanel = null;
                    videoViews.clear();
                }
            });
        }

        // Cerrar todos los recursos de manera ordenada
        try {
            if (videoOut != null) {
                try {
                    videoOut.close();
                } catch (IOException e) {
                    // Ignorar errores al cerrar videoOut
                }
            }
            if (dataOut != null) {
                try {
                    dataOut.close();
                } catch (IOException e) {
                    // Ignorar errores al cerrar dataOut
                }
            }
            if (dataIn != null) {
                try {
                    dataIn.close();
                } catch (IOException e) {
                    // Ignorar errores al cerrar dataIn
                }
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (videoSocket != null && !videoSocket.isClosed()) {
                videoSocket.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar conexion: " + e.getMessage());
        } finally {
            // Cerrar el pool de threads
            if (executorService != null) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }

        System.out.println("\nDesconectado del servidor. !Hasta luego!");
    }
    private void sendVideo() {
        VideoCapture cam = null;
        try {
            cam = new VideoCapture(0);
            if (!cam.isOpened()) {
                System.out.println("No se pudo abrir la camara.");
                videoActive = false;
                return;
            }

            Mat frame = new Mat();
            try {
                while (videoActive && running && cam.read(frame)) {
                    MatOfByte mob = new MatOfByte();
                    Imgcodecs.imencode(".jpg", frame, mob);
                    byte[] bytes = mob.toArray();

                    // Verificar que videoOut esté disponible antes de escribir
                    if (videoOut == null || videoSocket == null || videoSocket.isClosed()) {
                        break;
                    }

                    try {
                        // Enviar el frame al servidor
                        videoOut.writeInt(bytes.length);
                        videoOut.write(bytes);
                        videoOut.flush();
                    } catch (IOException e) {
                        System.err.println("Error enviando video: " + e.getMessage());
                        break;
                    }

                    // Mostrar el frame localmente (solo si la ventana está creada)
                    if (videoPanel != null) {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                        if (img != null) {
                            SwingUtilities.invokeLater(() -> {
                                if (videoPanel != null) {
                                    JLabel label = videoViews.computeIfAbsent("Yo", s -> {
                                        JLabel l = new JLabel();
                                        videoPanel.add(l);
                                        videoPanel.revalidate();
                                        videoPanel.repaint();
                                        return l;
                                    });
                                    label.setIcon(new ImageIcon(img));
                                }
                            });
                        }
                    }

                    Thread.sleep(50); // 20 FPS aprox
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Error en captura de video: " + e.getMessage());
            } finally {
                // Liberar el frame
                if (frame != null) {
                    frame.release();
                }
            }
        } catch (Exception e) {
            System.err.println("Error inicializando cámara: " + e.getMessage());
            videoActive = false;
        } finally {
            // Asegurar que la cámara se libere siempre
            if (cam != null) {
                try {
                    cam.release();
                } catch (Exception e) {
                    System.err.println("Error liberando cámara: " + e.getMessage());
                }
            }
            videoActive = false;
        }
    }
    private void receiveVideo() {
        DataInputStream videoIn = null;
        try {
            if (videoSocket == null || videoSocket.isClosed()) {
                return;
            }
            videoIn = new DataInputStream(videoSocket.getInputStream());
            
            while (videoActive && running && !videoSocket.isClosed()) {
                try {
                    int nameLen = videoIn.readInt();
                    byte[] nameBytes = new byte[nameLen];
                    videoIn.readFully(nameBytes);
                    String sender = new String(nameBytes);

                    int frameLen = videoIn.readInt();
                    byte[] frame = new byte[frameLen];
                    videoIn.readFully(frame);

                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(frame));
                    if (img != null && videoPanel != null) {
                        SwingUtilities.invokeLater(() -> {
                            if (videoPanel != null) {
                                JLabel label = videoViews.computeIfAbsent(sender, s -> {
                                    JLabel l = new JLabel("Cargando...");
                                    videoPanel.add(l);
                                    videoPanel.revalidate();
                                    videoPanel.repaint();
                                    return l;
                                });
                                label.setIcon(new ImageIcon(img));
                            }
                        });
                    }
                } catch (IOException e) {
                    if (running) {
                        System.out.println("Error al recibir video: " + e.getMessage());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error en recepción de video: " + e.getMessage());
        } finally {
            // Cerrar el stream de video
            if (videoIn != null) {
                try {
                    videoIn.close();
                } catch (IOException e) {
                    // Ignorar errores al cerrar
                }
            }
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        client.connect();
    }
}
