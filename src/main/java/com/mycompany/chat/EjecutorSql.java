/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.chat;

import com.mycompany.chat.config.ConfigManager;
import com.mycompany.chat.util.TestDataInitializer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ejecutor de scripts SQL para inicializar la base de datos.
 * Implementa patrón Singleton thread-safe usando double-checked locking.
 * 
 * @author nunez
 */
public class EjecutorSql {
    private static volatile EjecutorSql instance;
    
    private EjecutorSql() {
        // Constructor privado para prevenir instanciación directa
    }
    
    /**
     * Obtiene la instancia única del EjecutorSql (Singleton thread-safe).
     * 
     * @return instancia única de EjecutorSql
     */
    public static EjecutorSql getInstance() {
        if (instance == null) {
            synchronized (EjecutorSql.class) {
                if (instance == null) {
                    instance = new EjecutorSql();
                }
            }
        }
        return instance;
    }
    
    public void CreateDatabase(String usuario, String contraseña){
        String rutaArchivo = "src/main/resources/schema.sql";
        String url = "jdbc:mysql://localhost:3306/?allowMultiQueries=true";
        try (Connection conn = DriverManager.getConnection(url, usuario, contraseña)) {
            conn.setAutoCommit(true);

            String scriptSQL = new String(Files.readAllBytes(Paths.get(rutaArchivo)));

            String[] sentencias = scriptSQL.split(";");
            for (String sentencia : sentencias) {
                sentencia = sentencia.trim();
                if (!sentencia.isEmpty()) {
                    try (Statement st = conn.createStatement()) {
                        st.execute(sentencia);
                    } catch (SQLException e) {
                        System.err.println("[!] Error en: " + sentencia);
                        System.err.println(e.getMessage());
                    }
                }
            }

            System.out.println("\n[EXITO] Script ejecutado con exito.");
            ConfigManager.setDbUser(usuario);
            ConfigManager.setDbPassword(contraseña);
            
            // Crear usuarios de prueba automáticamente después de crear la base de datos
            System.out.println("\nCreando usuarios de prueba...");
            TestDataInitializer.initializeTestUsers();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
