package com.mycompany.chat.factory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Interfaz Factory para la creación de sockets.
 * Implementa el patrón Factory Method para encapsular la lógica de creación de sockets.
 * Permite diferentes implementaciones para testing, mockeo, o configuraciones especiales.
 */
public interface SocketFactory {
    
    /**
     * Crea un Socket de cliente para conectarse a un servidor.
     * 
     * @param host Dirección del servidor
     * @param port Puerto del servidor
     * @return Socket conectado al servidor
     * @throws IOException Si hay un error al crear o conectar el socket
     */
    Socket createClientSocket(String host, int port) throws IOException;
    
    /**
     * Crea un ServerSocket para escuchar conexiones entrantes.
     * 
     * @param port Puerto en el que escuchar
     * @return ServerSocket configurado
     * @throws IOException Si hay un error al crear el ServerSocket
     */
    ServerSocket createServerSocket(int port) throws IOException;
    
    /**
     * Crea un Socket de cliente para video.
     * Por defecto, usa el puerto base + offset para video.
     * 
     * @param host Dirección del servidor
     * @param basePort Puerto base del servidor
     * @param videoPortOffset Offset para el puerto de video
     * @return Socket conectado al servidor de video
     * @throws IOException Si hay un error al crear o conectar el socket
     */
    Socket createVideoClientSocket(String host, int basePort, int videoPortOffset) throws IOException;
    
    /**
     * Crea un ServerSocket para video.
     * Por defecto, usa el puerto base + offset para video.
     * 
     * @param basePort Puerto base del servidor
     * @param videoPortOffset Offset para el puerto de video
     * @return ServerSocket configurado para video
     * @throws IOException Si hay un error al crear el ServerSocket
     */
    ServerSocket createVideoServerSocket(int basePort, int videoPortOffset) throws IOException;
}

