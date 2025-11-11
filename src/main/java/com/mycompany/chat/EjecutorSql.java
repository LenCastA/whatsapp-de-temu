/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.chat;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author nunez
 */
public class EjecutorSql {
    private static EjecutorSql c;
    private EjecutorSql(){
        System.out.println("Instancia creada");
    }
    public static EjecutorSql CreateEjecutorSql(){
        if (c == null){
            c = new EjecutorSql();
        }
        return c;
    }
    
    public void CreateDatabase(String usuario, String contraseña){
        String rutaArchivo = "src/main/resources/schema.sql";
        String url = "jdbc:mysql://localhost:3306/?allowMultiQueries=true";
        try (Connection conn = DriverManager.getConnection(url, usuario, contraseña)) {
            conn.setAutoCommit(true);

            // 📖 Leer el archivo completo
            String scriptSQL = new String(Files.readAllBytes(Paths.get(rutaArchivo)));

            // 🧩 Ejecutar varias sentencias separadas por ';'
            String[] sentencias = scriptSQL.split(";");
            for (String sentencia : sentencias) {
                sentencia = sentencia.trim();
                if (!sentencia.isEmpty()) {
                    try (Statement st = conn.createStatement()) {
                        st.execute(sentencia);
                    } catch (SQLException e) {
                        System.err.println("⚠️ Error en: " + sentencia);
                        System.err.println(e.getMessage());
                    }
                }
            }

            System.out.println("\n🎉 Script ejecutado con éxito.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
