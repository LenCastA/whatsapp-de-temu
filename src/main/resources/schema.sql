DROP DATABASE IF EXISTS chatdb;
CREATE DATABASE IF NOT EXISTS chatdb;
USE chatdb;
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Insertar usuarios de prueba
INSERT INTO users (username, password)
VALUES ('Franz', 'franz123'),
    ('Alexis', 'alexis123'),
    ('Roy', 'charlie123'),
    ('Lenin', 'lenin123'),
    ('Admin', 'admin123');