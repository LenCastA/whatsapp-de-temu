package com.mycompany.chat.security;

/**
 * Valida entradas del usuario para prevenir problemas de seguridad y datos inválidos.
 */
public class InputValidator {
    
    // Constantes de validación
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 128;
    
    // Caracteres permitidos en username (solo alfanuméricos, guiones y guiones bajos)
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    
    /**
     * Valida un nombre de usuario.
     * 
     * @param username nombre de usuario a validar
     * @return mensaje de error si es inválido, null si es válido
     */
    public static String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "El nombre de usuario no puede estar vacío";
        }
        
        username = username.trim();
        
        if (username.length() < MIN_USERNAME_LENGTH) {
            return "El nombre de usuario debe tener al menos " + MIN_USERNAME_LENGTH + " caracteres";
        }
        
        if (username.length() > MAX_USERNAME_LENGTH) {
            return "El nombre de usuario no puede tener más de " + MAX_USERNAME_LENGTH + " caracteres";
        }
        
        if (!username.matches(USERNAME_PATTERN)) {
            return "El nombre de usuario solo puede contener letras, números, guiones (-) y guiones bajos (_)";
        }
        
        return null; // Válido
    }
    
    /**
     * Valida una contraseña.
     * 
     * @param password contraseña a validar
     * @return mensaje de error si es inválida, null si es válida
     */
    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "La contraseña no puede estar vacía";
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres";
        }
        
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return "La contraseña no puede tener más de " + MAX_PASSWORD_LENGTH + " caracteres";
        }
        
        // Verificar que tenga al menos una letra y un número (opcional, pero recomendado)
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        
        if (!hasLetter || !hasDigit) {
            return "La contraseña debe contener al menos una letra y un número";
        }
        
        return null; // Válida
    }
    
    /**
     * Valida un puerto.
     * 
     * @param portStr string del puerto
     * @return el puerto como entero si es válido, -1 si es inválido
     */
    public static int validatePort(String portStr) {
        if (portStr == null || portStr.trim().isEmpty()) {
            return -1; // Usar default
        }
        
        try {
            int port = Integer.parseInt(portStr.trim());
            if (port < 1 || port > 65535) {
                return -1;
            }
            return port;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Valida una dirección IP o hostname.
     * 
     * @param host dirección IP o hostname
     * @return mensaje de error si es inválido, null si es válido
     */
    public static String validateHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            return "El host no puede estar vacío";
        }
        
        host = host.trim();
        
        // Validación básica: no debe contener caracteres peligrosos
        if (host.contains(" ") || host.contains("\n") || host.contains("\r") || host.contains("\t")) {
            return "El host contiene caracteres inválidos";
        }
        
        if (host.length() > 255) {
            return "El host es demasiado largo";
        }
        
        return null; // Válido
    }
    
    /**
     * Sanitiza un string para prevenir inyección SQL (aunque usamos PreparedStatement,
     * es buena práctica sanitizar).
     * 
     * @param input string a sanitizar
     * @return string sanitizado
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        // Remover caracteres peligrosos
        return input.replace("'", "").replace("\"", "").replace(";", "").replace("--", "");
    }
}

