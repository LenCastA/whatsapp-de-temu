package com.mycompany.chat.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad para hashear y verificar contraseñas usando BCrypt.
 * BCrypt es un algoritmo de hash seguro que incluye sal automática.
 */
public class PasswordHasher {
    
    // Cost factor para BCrypt (10 es un buen balance entre seguridad y rendimiento)
    private static final int BCRYPT_ROUNDS = 10;
    
    /**
     * Hashea una contraseña en texto plano usando BCrypt.
     * 
     * @param plainPassword contraseña en texto plano
     * @return hash de la contraseña (incluye sal)
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula o vacía");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }
    
    /**
     * Verifica si una contraseña en texto plano coincide con un hash.
     * 
     * @param plainPassword contraseña en texto plano a verificar
     * @param hashedPassword hash almacenado en la base de datos
     * @return true si la contraseña coincide, false en caso contrario
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            // Si el hash es inválido o hay algún error, retornar false
            return false;
        }
    }
    
    /**
     * Verifica si un string es un hash BCrypt válido.
     * 
     * @param hash string a verificar
     * @return true si es un hash BCrypt válido
     */
    public static boolean isValidHash(String hash) {
        if (hash == null || hash.length() < 10) {
            return false;
        }
        // Los hashes BCrypt empiezan con $2a$, $2b$, o $2y$
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }
}

