package com.mycompany.chat.service;

import com.mycompany.chat.EjecutorSql;
import com.mycompany.chat.repository.DatabaseUserRepository;
import com.mycompany.chat.repository.UserRepository;
import com.mycompany.chat.security.InputValidator;
import java.sql.SQLException;

/**
 * Servicio para operaciones relacionadas con la base de datos.
 * Separa la lógica de negocio de la presentación.
 */
public class DatabaseService {
    private final UserRepository userRepository;

    public DatabaseService() {
        this(new DatabaseUserRepository());
    }

    public DatabaseService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
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
        return userRepository.testConnection();
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
        
        try {
            return userRepository.registerUser(username, password);
        } catch (SQLException e) {
            System.err.println("Error registrando usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Autentica a un usuario en la base de datos.
     *
     * @param username nombre de usuario
     * @param password contraseña
     * @return true si las credenciales son válidas
     */
    public boolean autenticarUsuario(String username, String password) {
        try {
            return userRepository.authenticate(username, password);
        } catch (SQLException e) {
            System.err.println("Error autenticando usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si existe un usuario específico.
     *
     * @param username nombre de usuario
     * @return true si el usuario existe
     */
    public boolean usuarioExiste(String username) {
        try {
            return userRepository.userExists(username);
        } catch (SQLException e) {
            System.err.println("Error verificando usuario: " + e.getMessage());
            return false;
        }
    }
}

