package com.mycompany.chat.service;

import com.mycompany.chat.Database;
import com.mycompany.chat.EjecutorSql;
import com.mycompany.chat.security.InputValidator;

/**
 * Servicio para operaciones relacionadas con la base de datos.
 * Separa la lógica de negocio de la presentación.
 */
public class DatabaseService {
    
    /**
     * Configura la base de datos ejecutando el script SQL.
     * 
     * @param usuario usuario de MySQL
     * @param contraseña contraseña de MySQL
     * @return true si la configuración fue exitosa
     */
    public boolean configurarBaseDatos(String usuario, String contraseña) {
        if (usuario == null || usuario.trim().isEmpty()) {
            usuario = "root";
        }
        
        try {
            EjecutorSql ejecutor = EjecutorSql.getInstance();
            ejecutor.CreateDatabase(usuario, contraseña);
            return true;
        } catch (Exception e) {
            System.err.println("Error configurando base de datos: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica la conexión a la base de datos.
     * 
     * @return true si la conexión es exitosa
     */
    public boolean verificarConexion() {
        return Database.testConnection();
    }
    
    /**
     * Registra un nuevo usuario en la base de datos.
     * 
     * @param username nombre de usuario
     * @param password contraseña
     * @return true si el registro fue exitoso
     */
    public boolean registrarUsuario(String username, String password) {
        // Validar entrada
        String usernameError = InputValidator.validateUsername(username);
        if (usernameError != null) {
            System.err.println("Error en username: " + usernameError);
            return false;
        }
        
        String passwordError = InputValidator.validatePassword(password);
        if (passwordError != null) {
            System.err.println("Error en password: " + passwordError);
            return false;
        }
        
        return Database.registerUser(username, password);
    }
}

