package com.mycompany.chat;

import com.mycompany.chat.ui.MenuController;
import java.util.Scanner;

/**
 * Punto de entrada principal de la aplicación.
 * Se encarga de inicializar OpenCV y delegar el control al MenuController.
 */
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
            MenuController menuController = new MenuController(scanner);
            menuController.mostrarMenuPrincipal();
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
}
