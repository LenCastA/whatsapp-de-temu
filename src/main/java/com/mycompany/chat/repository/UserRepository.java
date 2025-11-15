package com.mycompany.chat.repository;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interfaz Repository para el acceso a datos de usuarios.
 * 
 * Este patrón Repository:
 * - Abstrae el acceso a la base de datos
 * - Facilita el testing con mocks
 * - Permite cambiar la implementación sin afectar el código cliente
 * - Centraliza la lógica de acceso a datos
 */
public interface UserRepository {
    
    /**
     * Autentica un usuario con nombre de usuario y contraseña.
     * 
     * @param username Nombre de usuario
     * @param password Contraseña en texto plano
     * @return true si las credenciales son válidas
     * @throws SQLException Si hay un error de base de datos
     */
    boolean authenticate(String username, String password) throws SQLException;
    
    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * @param username Nombre de usuario
     * @param password Contraseña en texto plano (será hasheada)
     * @return true si el registro fue exitoso
     * @throws SQLException Si hay un error de base de datos
     */
    boolean registerUser(String username, String password) throws SQLException;
    
    /**
     * Verifica si un usuario existe en la base de datos.
     * 
     * @param username Nombre de usuario
     * @return true si el usuario existe
     * @throws SQLException Si hay un error de base de datos
     */
    boolean userExists(String username) throws SQLException;
    
    /**
     * Obtiene una conexión a la base de datos.
     * 
     * @return Conexión a la base de datos
     * @throws SQLException Si hay un error al conectar
     */
    Connection getConnection() throws SQLException;
    
    /**
     * Verifica la conexión a la base de datos.
     * 
     * @return true si la conexión es exitosa
     */
    boolean testConnection();
}

