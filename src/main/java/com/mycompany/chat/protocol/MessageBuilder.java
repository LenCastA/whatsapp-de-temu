package com.mycompany.chat.protocol;

import com.mycompany.chat.util.Constants;

/**
 * Builder Pattern para construir mensajes del protocolo de forma segura.
 * 
 * Ventajas:
 * - Evita errores de formato en la construcción de mensajes
 * - Código más legible y mantenible
 * - Validación automática de parámetros
 * - Inmutabilidad opcional
 */
public class MessageBuilder {
    private final StringBuilder message;
    
    private MessageBuilder() {
        this.message = new StringBuilder();
    }
    
    /**
     * Crea un nuevo MessageBuilder.
     * 
     * @return Nueva instancia de MessageBuilder
     */
    public static MessageBuilder create() {
        return new MessageBuilder();
    }
    
    /**
     * Agrega el tipo de mensaje (comando o respuesta).
     * 
     * @param type Tipo del mensaje (ej: "LOGIN", "MSG", "OK", "ERROR")
     * @return this para method chaining
     */
    public MessageBuilder withType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de mensaje no puede ser nulo o vacío");
        }
        message.append(type);
        return this;
    }
    
    /**
     * Agrega un parámetro al mensaje.
     * 
     * @param param Parámetro a agregar
     * @return this para method chaining
     */
    public MessageBuilder withParam(String param) {
        if (param == null) {
            param = "";
        }
        message.append(Constants.PROTOCOL_SEPARATOR).append(param);
        return this;
    }
    
    /**
     * Agrega múltiples parámetros al mensaje.
     * 
     * @param params Parámetros a agregar
     * @return this para method chaining
     */
    public MessageBuilder withParams(String... params) {
        for (String param : params) {
            withParam(param);
        }
        return this;
    }
    
    /**
     * Construye el mensaje final.
     * 
     * @return El mensaje construido como String
     */
    public String build() {
        if (message.length() == 0) {
            throw new IllegalStateException("No se puede construir un mensaje vacío");
        }
        return message.toString();
    }
    
    // Métodos de conveniencia para mensajes comunes
    
    /**
     * Construye un mensaje de login.
     * 
     * @param username Nombre de usuario
     * @param password Contraseña
     * @return Mensaje de login formateado
     */
    public static String buildLogin(String username, String password) {
        return create()
            .withType(Constants.CMD_LOGIN)
            .withParams(username, password)
            .build();
    }
    
    /**
     * Construye un mensaje de texto.
     * 
     * @param recipient Destinatario
     * @param message Mensaje de texto
     * @return Mensaje formateado
     */
    public static String buildMessage(String recipient, String message) {
        return create()
            .withType(Constants.CMD_MSG)
            .withParams(recipient, message)
            .build();
    }
    
    /**
     * Construye una respuesta OK.
     * 
     * @param command Comando al que responde
     * @param message Mensaje adicional
     * @return Respuesta OK formateada
     */
    public static String buildOk(String command, String message) {
        return create()
            .withType(Constants.RESP_OK)
            .withParam(command)
            .withParam(message)
            .build();
    }
    
    /**
     * Construye una respuesta de error.
     * 
     * @param errorMessage Mensaje de error
     * @return Respuesta de error formateada
     */
    public static String buildError(String errorMessage) {
        return create()
            .withType(Constants.RESP_ERROR)
            .withParam(errorMessage)
            .build();
    }
    
    /**
     * Construye un mensaje del servidor.
     * 
     * @param serverMessage Mensaje del servidor
     * @return Mensaje del servidor formateado
     */
    public static String buildServerMessage(String serverMessage) {
        return create()
            .withType(Constants.RESP_SERVER)
            .withParam(serverMessage)
            .build();
    }
}

