package com.mycompany.chat;

import java.sql.*;

public class Database {
    // Tratar de dinamizar
    private static final String URL = "jdbc:mysql://localhost:3306/chatdb?useSSL=false&serverTimezone=UTC";
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

    // Autenticación
    public static boolean authenticate(String username, String password) throws SQLException {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return false;
        }

        String sql = "SELECT password FROM users WHERE username = '" + username + "'";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) {
                return false;
            }

            String storedPassword = rs.getString("password");
            return password.equals(storedPassword);
        }
    }

    // Verificación de la conexión a la base de datos
    public static boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Error al conectar con la base de datos: " + e.getMessage());
            return false;
        }
    }
}
