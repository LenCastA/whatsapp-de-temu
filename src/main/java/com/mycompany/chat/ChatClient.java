package com.mycompany.chat;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;

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
            frame.add(videoLabel);
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

            // Esperar respuesta
            Thread.sleep(1000);
            System.out.println("-------------------------------------\n");

            // Manejar input del usuario
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

    // Maneja la entrada del usuario
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
            case "/video":
                if (!videoActive) {
                    videoActive = true;
                    sendMessage("VIDEO|START"); // avisar al servidor
                    new Thread(this::sendVideo).start();     // 👈 empieza a enviar cámara
                    new Thread(this::receiveVideo).start();  // 👈 empieza a recibir frames
                    System.out.println("📹 Video activado.");
                } else {
                    videoActive = false;
                    sendMessage("VIDEO|STOP");
                    System.out.println("📴 Video detenido.");
                }

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
            int fileSize = fileData.length;

            if (fileData.length > 50 * 1024 * 1024) { // 50MB límite
                System.out.println("Archivo demasiado grande (max 50MB)");
                return;
            }

            // Avisar al servidor que viene un archivo
            sendMessage("FILE|" + fileName +"|"+ fileSize);
            try {
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
            }

            // Enviar tamaño y datos
            dataOut.write(fileData);
            dataOut.flush();

            System.out.println("Enviando archivo: " + fileName + " (" + fileSize + " bytes)");

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
        System.out.println();
        System.out.println("  /logout");
        System.out.println("    Cierra la sesión y sale del chat");
        System.out.println();
        System.out.println("  /help");
        System.out.println("    Muestra esta ayuda");
        System.out.println("─────────────────────────────────────────\n");
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
                MatOfByte mob = new MatOfByte();
                Imgcodecs.imencode(".jpg", frame, mob);
                byte[] bytes = mob.toArray();

                // Enviar el frame al servidor
                videoOut.writeInt(bytes.length);
                videoOut.write(bytes);
                videoOut.flush();

                // Mostrar el frame localmente 👇
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) {
                    SwingUtilities.invokeLater(() -> {
                        videoLabel.setIcon(new ImageIcon(img));
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
                int len;
                try {
                    len = videoIn.readInt(); // lee longitud del frame
                } catch (EOFException e) {
                    System.out.println("📴 Conexión de video cerrada por el servidor.");
                    break;
                }

                byte[] bytes = new byte[len];
                videoIn.readFully(bytes);

                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) {
                    SwingUtilities.invokeLater(() -> {
                        videoLabel.setIcon(new ImageIcon(img));
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
    // main de prueba (puedes usar Main.java también)
    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));
        client.connect();
    }
}
