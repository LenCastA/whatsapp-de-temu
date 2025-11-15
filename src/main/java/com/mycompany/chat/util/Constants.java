package com.mycompany.chat.util;

/**
 * Constantes de la aplicación para configuración y límites.
 * Centraliza valores mágicos para facilitar mantenimiento.
 */
public class Constants {
    
    // Configuración de red
    public static final int DEFAULT_PORT = 9000;
    public static final int DEFAULT_VIDEO_PORT_OFFSET = 1;
    public static final String DEFAULT_HOST = "localhost";
    
    // Límites de tamaño
    public static final int MAX_FILE_SIZE_MB = 50;
    public static final long MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024L * 1024L;
    public static final int MAX_MESSAGE_LENGTH = 10240; // 10KB
    
    // Configuración de video
    public static final int VIDEO_FPS_DELAY_MS = 50; // ~20 FPS
    public static final int VIDEO_CAMERA_INDEX = 0;
    
    // Timeouts y delays
    public static final int LOGIN_TIMEOUT_MS = 5000; // 5 segundos
    public static final int MESSAGE_SYNC_DELAY_MS = 100;
    public static final int FILE_SEND_DELAY_MS = 100;
    
    // Configuración de threads
    public static final int CLIENT_THREAD_POOL_SIZE = 10;
    public static final int SERVER_THREAD_POOL_SIZE = 50;
    public static final int THREAD_SHUTDOWN_TIMEOUT_SECONDS = 5;
    
    // Validación de usuarios
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 128;
    
    // Constantes de protocolo (para evitar strings mágicos)
    public static final String PROTOCOL_SEPARATOR = "|";
    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_LOGOUT = "LOGOUT";
    public static final String CMD_MSG = "MSG";
    public static final String CMD_FILE = "FILE";
    public static final String CMD_VIDEO = "VIDEO";
    public static final String CMD_USERS = "USERS";
    public static final String RESP_OK = "OK";
    public static final String RESP_ERROR = "ERROR";
    public static final String RESP_SERVER = "SERVER";
    public static final String RESP_SYSTEM = "SYSTEM";
    
    private Constants() {
        // Clase de utilidad, no instanciable
        throw new AssertionError("No se debe instanciar Constants");
    }
}

