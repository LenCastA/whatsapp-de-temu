package com.mycompany.chat.repository;

import com.mycompany.chat.config.ConfigManager;
import com.mycompany.chat.security.PasswordHasher;
import java.sql.*;

/**
 * Implementación concreta del Repository usando base de datos MySQL.
 * 
 * Esta implementación:
 * - Encapsula toda la lógica de acceso a datos
 * - Maneja conexiones y transacciones
 * - Proporciona una interfaz limpia para operaciones de usuarios
 */
public class DatabaseUserRepository implements UserRepository {
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver de MySQL");
            e.printStackTrace();
        }
    }
    
    private String getUrl() {
        return ConfigManager.getDbUrl();
    }
    
    private String getUser() {
        return ConfigManager.getDbUser();
    }
    
    private String getPassword() {
        return ConfigManager.getDbPassword();
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }
    
    @Override
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Error al conectar con la base de datos: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean authenticate(String username, String password) throws SQLException {
        if (username == null || password == null ||
            username.isEmpty() || password.isEmpty()) {
            return false;
        }

        String sql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // usuario no existe
                }

                String storedPasswordHash = rs.getString("password");
                // Verificar contraseña usando BCrypt
                return PasswordHasher.verifyPassword(password, storedPasswordHash);
            }
        }
    }
    
    @Override
    public boolean registerUser(String username, String password) throws SQLException {
        if (username == null || password == null ||
            username.isEmpty() || password.isEmpty()) {
            return false;
        }

        // Hashear la contraseña antes de almacenarla
        String hashedPassword = PasswordHasher.hashPassword(password);

        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, hashedPassword);

            ps.executeUpdate();
            return true;
        }
    }
    
    @Override
    public boolean userExists(String username) throws SQLException {
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }
}

