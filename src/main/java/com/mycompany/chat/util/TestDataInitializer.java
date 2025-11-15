package com.mycompany.chat.util;

import com.mycompany.chat.Database;
import com.mycompany.chat.security.PasswordHasher;

/**
 * Utilidad para inicializar datos de prueba en la base de datos.
 * Crea usuarios de prueba con contraseñas hasheadas para facilitar las pruebas.
 */
public class TestDataInitializer {
    
    // Usuarios de prueba con sus contraseñas
    private static final String[][] TEST_USERS = {
        {"Franz", "franz123"},
        {"Alexis", "alexis123"},
        {"Roy", "charlie123"},
        {"Lenin", "lenin123"},
        {"Admin", "admin123"}
    };
    
    /**
     * Inicializa usuarios de prueba en la base de datos.
     * Solo crea usuarios que no existan previamente.
     * 
     * @return número de usuarios creados exitosamente
     */
    public static int initializeTestUsers() {
        int created = 0;
        
        System.out.println("Inicializando usuarios de prueba...");
        
        for (String[] userData : TEST_USERS) {
            String username = userData[0];
            String password = userData[1];
            
            try {
                // Verificar si el usuario ya existe intentando autenticarse
                // Si no existe, intentar crearlo
                if (!Database.authenticate(username, password)) {
                    // El usuario no existe o la contraseña es incorrecta
                    // Intentar crear el usuario
                    if (Database.registerUser(username, password)) {
                        System.out.println("[OK] Usuario creado: " + username);
                        created++;
                    } else {
                        System.err.println("[ERROR] Error al crear usuario: " + username);
                    }
                } else {
                    System.out.println("[->] Usuario ya existe: " + username);
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Error procesando usuario " + username + ": " + e.getMessage());
            }
        }
        
        System.out.println("\nUsuarios de prueba inicializados: " + created + " nuevos, " + 
                          (TEST_USERS.length - created) + " ya existían.");
        
        return created;
    }
    
    /**
     * Genera hashes BCrypt para las contraseñas de prueba.
     * Útil para crear scripts SQL con hashes pre-generados.
     */
    public static void printTestUserHashes() {
        System.out.println("-- Hashes BCrypt para usuarios de prueba:");
        System.out.println("-- Usar estos hashes en scripts SQL si es necesario\n");
        
        for (String[] userData : TEST_USERS) {
            String username = userData[0];
            String password = userData[1];
            String hash = PasswordHasher.hashPassword(password);
            
            System.out.println("-- Usuario: " + username + ", Password: " + password);
            System.out.println("INSERT INTO users (username, password) VALUES ('" + 
                             username + "', '" + hash + "');");
            System.out.println();
        }
    }
    
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--print-hashes")) {
            printTestUserHashes();
        } else {
            initializeTestUsers();
        }
    }
}

