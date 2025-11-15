package com.mycompany.chat.util;

import com.mycompany.chat.security.PasswordHasher;

/**
 * Utilidad para generar hashes de contraseñas.
 * Se puede usar para generar hashes para el schema.sql o para migración de datos.
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java PasswordHashGenerator <contraseña1> [contraseña2] ...");
            System.out.println("Genera hashes BCrypt para las contraseñas proporcionadas.");
            return;
        }
        
        for (String password : args) {
            String hash = PasswordHasher.hashPassword(password);
            System.out.println("Contraseña: " + password);
            System.out.println("Hash: " + hash);
            System.out.println("Verificacion: " + (PasswordHasher.verifyPassword(password, hash) ? "[OK]" : "[ERROR]"));
            System.out.println();
        }
    }
}

