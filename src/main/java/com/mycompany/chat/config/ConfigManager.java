package com.mycompany.chat.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Gestiona la configuración de la aplicación, incluyendo credenciales de base de datos.
 * Las credenciales se almacenan en un archivo de propiedades en lugar de hardcodearse.
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();
    private static boolean loaded = false;
    
    // Valores por defecto (solo si no existe el archivo de configuración)
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASSWORD = "";
    private static final String DEFAULT_DB_URL = "jdbc:mysql://localhost:3306/chatdb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    
    /**
     * Carga la configuración desde el archivo, o crea uno con valores por defecto si no existe.
     */
    public static void loadConfig() {
        if (loaded) {
            return;
        }
        
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
            loaded = true;
        } catch (IOException e) {
            // Si el archivo no existe, crear uno con valores por defecto
            createDefaultConfig();
            loaded = true;
        }
    }
    
    /**
     * Crea un archivo de configuración con valores por defecto.
     */
    private static void createDefaultConfig() {
        properties.setProperty("db.user", DEFAULT_DB_USER);
        properties.setProperty("db.password", DEFAULT_DB_PASSWORD);
        properties.setProperty("db.url", DEFAULT_DB_URL);
        saveConfig();
    }
    
    /**
     * Guarda la configuración actual en el archivo.
     */
    public static void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Configuración de la aplicación de chat");
        } catch (IOException e) {
            System.err.println("Error al guardar configuración: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene el usuario de la base de datos.
     */
    public static String getDbUser() {
        loadConfig();
        return properties.getProperty("db.user", DEFAULT_DB_USER);
    }
    
    /**
     * Obtiene la contraseña de la base de datos.
     */
    public static String getDbPassword() {
        loadConfig();
        return properties.getProperty("db.password", DEFAULT_DB_PASSWORD);
    }
    
    /**
     * Obtiene la URL de la base de datos.
     */
    public static String getDbUrl() {
        loadConfig();
        return properties.getProperty("db.url", DEFAULT_DB_URL);
    }
    
    /**
     * Establece el usuario de la base de datos.
     */
    public static void setDbUser(String user) {
        loadConfig();
        properties.setProperty("db.user", user);
        saveConfig();
    }
    
    /**
     * Establece la contraseña de la base de datos.
     */
    public static void setDbPassword(String password) {
        loadConfig();
        properties.setProperty("db.password", password);
        saveConfig();
    }
    
    /**
     * Establece la URL de la base de datos.
     */
    public static void setDbUrl(String url) {
        loadConfig();
        properties.setProperty("db.url", url);
        saveConfig();
    }
    
    /**
     * Verifica si la configuración ha sido cargada.
     */
    public static boolean isLoaded() {
        return loaded;
    }
}

