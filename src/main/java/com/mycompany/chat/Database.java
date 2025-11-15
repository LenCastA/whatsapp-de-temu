package com.mycompany.chat;

import com.mycompany.chat.config.ConfigManager;
import com.mycompany.chat.security.PasswordHasher;
import java.sql.*;

public class Database {
    // La URL ahora se obtiene de ConfigManager
    private static String getUrl() {
        return ConfigManager.getDbUrl();
    }
    
    private static String getUser() {
        return ConfigManager.getDbUser();
    }
    
    private static String getPassword() {
        return ConfigManager.getDbPassword();
    }
    
    public static void setUser(String new_user){
        ConfigManager.setDbUser(new_user);
    }
    
    public static void setPassword(String new_password){
        ConfigManager.setDbPassword(new_password);
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver de MySQL");
            e.printStackTrace();
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    public static boolean authenticate(String username, String password) {
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

        } catch (SQLException e) {
            System.err.println("Error en authenticate(): " + e.getMessage());
            return false;
        }
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Error al conectar con la base de datos: " + e.getMessage());
            return false;
        }
    }
    // Registrar nuevo usuario en la BD
    public static boolean registerUser(String username, String password) {
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
            ps.setString(2, hashedPassword); // Almacenar hash en lugar de texto plano

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error en registerUser(): " + e.getMessage());
            return false;
        }
    }

}
