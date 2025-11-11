package com.mycompany.chat;

import java.sql.*;

public class Database {
    // Tratar de dinamizar
    private static final String URL = "jdbc:mysql://localhost:3306/chatdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String USER = "root";
    private static String PASS = "root";
    
    public static void setUser(String new_user){
        Database.USER = new_user;    
    }
    public static void setPassword(String new_password){
        Database.PASS = new_password;
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
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // Autenticación usando PreparedStatement (sin SQL injection)
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

                String storedPassword = rs.getString("password");
                // Comparación en texto plano (porque en schema.sql también están en texto plano)
                return password.equals(storedPassword);
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

        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password); // por ahora texto plano

            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error en registerUser(): " + e.getMessage());
            return false;
        }
    }

}
