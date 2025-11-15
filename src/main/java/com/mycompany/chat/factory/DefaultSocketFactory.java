package com.mycompany.chat.factory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.mycompany.chat.util.Constants;

/**
 * Implementación por defecto del Factory Method para la creación de sockets.
 * Crea sockets estándar de Java sin configuraciones especiales.
 */
public class DefaultSocketFactory implements SocketFactory {
    
    /**
     * Crea un Socket de cliente estándar.
     */
    @Override
    public Socket createClientSocket(String host, int port) throws IOException {
        return new Socket(host, port);
    }
    
    /**
     * Crea un ServerSocket estándar.
     */
    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port);
    }
    
    /**
     * Crea un Socket de cliente para video usando el puerto base + offset.
     */
    @Override
    public Socket createVideoClientSocket(String host, int basePort, int videoPortOffset) throws IOException {
        int videoPort = basePort + videoPortOffset;
        return new Socket(host, videoPort);
    }
    
    /**
     * Crea un ServerSocket para video usando el puerto base + offset.
     */
    @Override
    public ServerSocket createVideoServerSocket(int basePort, int videoPortOffset) throws IOException {
        int videoPort = basePort + videoPortOffset;
        return new ServerSocket(videoPort);
    }
    
    /**
     * Método de conveniencia para crear un socket de video usando las constantes por defecto.
     * 
     * @param host Dirección del servidor
     * @param basePort Puerto base del servidor
     * @return Socket conectado al servidor de video
     * @throws IOException Si hay un error al crear o conectar el socket
     */
    public Socket createVideoClientSocket(String host, int basePort) throws IOException {
        return createVideoClientSocket(host, basePort, Constants.DEFAULT_VIDEO_PORT_OFFSET);
    }
    
    /**
     * Método de conveniencia para crear un ServerSocket de video usando las constantes por defecto.
     * 
     * @param basePort Puerto base del servidor
     * @return ServerSocket configurado para video
     * @throws IOException Si hay un error al crear el ServerSocket
     */
    public ServerSocket createVideoServerSocket(int basePort) throws IOException {
        return createVideoServerSocket(basePort, Constants.DEFAULT_VIDEO_PORT_OFFSET);
    }
}

