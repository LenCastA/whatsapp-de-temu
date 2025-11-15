package com.mycompany.chat;

import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
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
import com.mycompany.chat.factory.DefaultSocketFactory;
import com.mycompany.chat.factory.SocketFactory;
import com.mycompany.chat.util.Constants;

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
    private CountDownLatch loginLatch; // Sincronización de login
    
    private final Map<String, JLabel> videoViews = new ConcurrentHashMap<>();
    private JFrame videoFrame; // Ventana de video (se crea solo cuando se inicia video)
    private JPanel videoPanel; // Panel de video (se crea solo cuando se inicia video)
    private ExecutorService executorService; // Pool de threads para gestionar hilos
    private final Object dataOutLock = new Object(); // Sincronización para escritura de mensajes y archivos
    private final SocketFactory socketFactory; // Factory para crear sockets

    // Constructor por defecto => localhost:9000
    public ChatClient() {
        this("localhost", 9000);
        this.videoActive = false;
    }

    // Constructor configurable (lo usa Main)
    public ChatClient(String host, int port) {
        this(host, port, new DefaultSocketFactory());
    }
    
    // Constructor con factory personalizado
    public ChatClient(String host, int port, SocketFactory socketFactory) {
        this.videoActive = false;
        this.host = host;
        this.port = port;
        this.scanner = new Scanner(System.in);
        this.running = true;
        this.executorService = Executors.newFixedThreadPool(Constants.CLIENT_THREAD_POOL_SIZE);
        this.socketFactory = socketFactory;
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

            // Usar el factory para crear los sockets
            socket = socketFactory.createClientSocket(host, port);
            videoSocket = socketFactory.createVideoClientSocket(host, port, Constants.DEFAULT_VIDEO_PORT_OFFSET);
            videoOut = new DataOutputStream(videoSocket.getOutputStream());
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());

            System.out.println("Conectado al servidor!\n");

            // Inicializar latch para sincronización de login
            loginLatch = new CountDownLatch(1);

            // Hilo para recibir mensajes del servidor (gestionado por ExecutorService)
            executorService.submit(this::receiveMessages);

            // Pequeña pausa para asegurar que el hilo de recepción esté listo
            // (no es para sincronización, solo para inicialización)
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // Pedir credenciales
            System.out.println("-------------------------------------");
            System.out.println("           INICIAR SESION");
            System.out.println("-------------------------------------");
            System.out.print("Usuario: ");
            String username = scanner.nextLine().trim();

            System.out.print("Contrasena: ");
            String password = scanner.nextLine().trim();

            // Enviar login al servidor según nuevo formato (debe ser síncrono)
            sendMessageBlocking(Constants.CMD_LOGIN + Constants.PROTOCOL_SEPARATOR + username + 
                       Constants.PROTOCOL_SEPARATOR + password);

            // Esperar respuesta del login con timeout
            boolean loginReceived = false;
            try {
                loginReceived = loginLatch.await(Constants.LOGIN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Error: Login interrumpido");
                return;
            }
            
            if (!loginReceived) {
                System.err.println("Error: Timeout esperando confirmación de login. Cerrando...");
                return;
            }
            
            System.out.println("-------------------------------------\n");
            
            // Ahora pedir que elija destinatario
            selectRecipientAndShowMenu();

        } catch (IOException e) {
            System.err.println("Error de conexion: " + e.getMessage());
            System.err.println("Asegurate de que el servidor este corriendo.");
        } finally {
            disconnect();
        }
    }

    // Recibe mensajes del servidor (hilo separado)
    private void receiveMessages() {
        try {
            while (running ) {
                String message = dataIn.readUTF();
                processIncomingMessage(message);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("\nConexion cerrada por el servidor");                
            }
        } finally {
            running = false;
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
                if (parts.length >= 2 && Constants.CMD_LOGIN.equals(parts[1])) {
                    System.out.println("\n" + (parts.length > 2 ? parts[2] : "Login exitoso"));
                    System.out.println("\n====================================================");
                    System.out.println("  !Bienvenido! Ahora debes seleccionar un destinatario");
                    System.out.println("====================================================\n");
                    // Liberar el latch para indicar que el login fue exitoso
                    if (loginLatch != null) {
                        loginLatch.countDown();
                    }
                } else if (parts.length >= 2 && Constants.CMD_MSG.equals(parts[1])) {
                    // Confirmación de mensaje enviado
                    if (parts.length > 2) {
                        System.out.println("[OK] " + parts[2]);
                    }
                } else if (parts.length > 1) {
                    System.out.println("[OK] " + parts[1]);
                }
                break;

            case "ERROR":
                String errorMsg = parts.length > 1 ? parts[1] : "Error desconocido";
                System.out.println("Error: " + errorMsg);
                // Si hay un error durante el login, liberar el latch para evitar bloqueo
                if (loginLatch != null && loginLatch.getCount() > 0) {
                    loginLatch.countDown();
                }
                break;

            case "MSG":
                if (parts.length >= 3) {
                    String sender = parts[1];
                    String msg = parts[2];
                    // Validar tamaño del mensaje
                    if (msg.length() > Constants.MAX_MESSAGE_LENGTH) {
                        System.err.println("Advertencia: Mensaje recibido excede el tamaño máximo");
                        break;
                    }
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
            
            // Solicitar lista de usuarios (debe ser síncrono)
            sendMessageBlocking(Constants.CMD_USERS);
            
            // No necesitamos esperar aquí - el mensaje se mostrará cuando llegue
            // El usuario puede ingresar el destinatario inmediatamente
            
            System.out.print("\nIngresa el nombre del destinatario (o 'salir' para cerrar): ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("salir") || input.equalsIgnoreCase("exit")) {
                sendMessageBlocking(Constants.CMD_LOGOUT);
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
            if (videoActive) {
                System.out.println("Estado: [VIDEO ACTIVO] - Videollamada en curso");
            }
            System.out.println("----------------------------------------------------");
            System.out.println("NOTA: Mensajes, archivos y video se envian");
            System.out.println("      simultaneamente usando multihilos.");
            System.out.println("      Escribe 'volver' en cualquier momento para");
            System.out.println("      regresar al menu desde cualquier opcion.");
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
        
        sendMessageBlocking(Constants.CMD_VIDEO + Constants.PROTOCOL_SEPARATOR + "START" + 
                   Constants.PROTOCOL_SEPARATOR + recipient);
        executorService.submit(this::sendVideo);
        executorService.submit(this::receiveVideo);
    }
    
    public void stopVideoCall() {
        videoActive = false;
        sendMessageBlocking(Constants.CMD_VIDEO + Constants.PROTOCOL_SEPARATOR + "STOP");
        
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
    
    // Envía un archivo al servidor (al destinatario actual) - versión síncrona interna
    private void sendFileSync(String filePath) {
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

            if (fileData.length > Constants.MAX_FILE_SIZE_BYTES) {
                System.out.println("Archivo demasiado grande (max " + Constants.MAX_FILE_SIZE_MB + "MB)");
                return;
            }

            // Avisar al servidor que viene un archivo (formato: FILE|destinatario|nombre|tamaño)
            sendMessageSync(Constants.CMD_FILE + Constants.PROTOCOL_SEPARATOR + currentRecipient + 
                       Constants.PROTOCOL_SEPARATOR + fileName + 
                       Constants.PROTOCOL_SEPARATOR + fileSize);
            
            // Enviar tamaño y datos (sincronizado para evitar conflictos con mensajes)
            synchronized (dataOutLock) {
                if (dataOut != null && !socket.isClosed()) {
                    dataOut.write(fileData);
                    dataOut.flush();
                }
            }

            System.out.println("[ARCHIVO] Enviando archivo a " + currentRecipient + ": " + fileName + " (" + fileSize + " bytes)");

        } catch (IOException e) {
            System.err.println("[ERROR] Error enviando archivo: " + e.getMessage());
        }
    }
    
    // Envía un archivo al servidor en un hilo separado (versión pública asíncrona)
    public void sendFile(String filePath) {
        executorService.submit(() -> {
            sendFileSync(filePath);
        });
    }

    // Envía un mensaje al servidor - versión síncrona interna (thread-safe)
    private void sendMessageSync(String message) {
        synchronized (dataOutLock) {
            try{
                if (dataOut != null && !socket.isClosed()) {
                    dataOut.writeUTF(message);
                    dataOut.flush();
                }
            } catch (IOException e){
                System.err.println("[ERROR] Error enviando mensaje: "+e.getMessage());
            }
        }
    }
    
    // Envía un mensaje al servidor en un hilo separado (versión pública asíncrona)
    public void sendMessage(String message) {
        executorService.submit(() -> {
            sendMessageSync(message);
        });
    }
    
    // Envía un mensaje de forma síncrona (para comandos críticos como LOGIN, LOGOUT)
    public void sendMessageBlocking(String message) {
        sendMessageSync(message);
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
                    if (!executorService.awaitTermination(Constants.THREAD_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                        if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                            System.err.println("Advertencia: Algunos threads del cliente no terminaron correctamente");
                        }
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
        Mat frame = null;
        int consecutiveFailures = 0;
        final int MAX_CONSECUTIVE_FAILURES = 10; // Máximo de fallos consecutivos antes de detener
        
        try {
            // Intentar abrir la cámara con un pequeño delay para inicialización
            cam = new VideoCapture(Constants.VIDEO_CAMERA_INDEX);
            
            // Dar tiempo a la cámara para inicializarse (especialmente importante en Windows)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            if (!cam.isOpened()) {
                System.err.println("\n[ERROR] No se pudo abrir la cámara.");
                System.err.println("Posibles causas:");
                System.err.println("  - La cámara está siendo usada por otra aplicación");
                System.err.println("  - La cámara no está conectada o no funciona");
                System.err.println("  - Problemas con los drivers de la cámara");
                System.err.println("  - Permisos insuficientes para acceder a la cámara\n");
                videoActive = false;
                sendMessageBlocking(Constants.CMD_VIDEO + Constants.PROTOCOL_SEPARATOR + "STOP");
                return;
            }

            frame = new Mat();
            System.out.println("[VIDEO] Cámara iniciada correctamente. Transmitiendo...\n");
            
            try {
                while (videoActive && running) {
                    // Intentar leer un frame
                    boolean frameRead = cam.read(frame);
                    
                    if (!frameRead || frame.empty()) {
                        consecutiveFailures++;
                        
                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                            System.err.println("\n[ERROR] No se pueden capturar más frames de la cámara.");
                            System.err.println("La cámara puede haberse desconectado o estar siendo usada por otra aplicación.\n");
                            break;
                        }
                        
                        // Esperar un poco antes de reintentar
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }
                    
                    // Frame leído correctamente, resetear contador
                    consecutiveFailures = 0;

                    // Verificar que videoOut esté disponible antes de escribir
                    if (videoOut == null || videoSocket == null || videoSocket.isClosed()) {
                        System.err.println("[ERROR] Conexión de video perdida.");
                        break;
                    }

                    try {
                        // Codificar frame a JPEG
                        MatOfByte mob = new MatOfByte();
                        Imgcodecs.imencode(".jpg", frame, mob);
                        byte[] bytes = mob.toArray();
                        
                        if (bytes.length == 0) {
                            continue; // Frame vacío, saltar
                        }

                        // Enviar el frame al servidor
                        videoOut.writeInt(bytes.length);
                        videoOut.write(bytes);
                        videoOut.flush();
                        
                        // Mostrar el frame localmente (solo si la ventana está creada)
                        if (videoPanel != null) {
                            try {
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
                            } catch (Exception e) {
                                // Error al mostrar frame local, continuar enviando
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("[ERROR] Error enviando video: " + e.getMessage());
                        break;
                    } catch (Exception e) {
                        System.err.println("[ERROR] Error procesando frame: " + e.getMessage());
                        // Continuar intentando
                        continue;
                    }

                    // Control de FPS
                    try {
                        Thread.sleep(Constants.VIDEO_FPS_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Error en captura de video: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // Liberar el frame
                if (frame != null) {
                    frame.release();
                }
            }
        } catch (Exception e) {
            System.err.println("\n[ERROR] Error inicializando cámara: " + e.getMessage());
            System.err.println("Detalles: " + e.getClass().getSimpleName());
            e.printStackTrace();
            videoActive = false;
        } finally {
            // Guardar estado antes de cambiar
            boolean wasActive = videoActive;
            videoActive = false;
            
            // Asegurar que la cámara se libere siempre
            if (cam != null) {
                try {
                    cam.release();
                    System.out.println("[VIDEO] Cámara liberada correctamente.");
                } catch (Exception e) {
                    System.err.println("[ERROR] Error liberando cámara: " + e.getMessage());
                }
            }
            
            // Notificar al servidor que se detuvo el video si estaba activo
            if (wasActive) {
                try {
                    sendMessageBlocking(Constants.CMD_VIDEO + Constants.PROTOCOL_SEPARATOR + "STOP");
                } catch (Exception e) {
                    // Ignorar si no se puede enviar el mensaje
                }
            }
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


