DROP DATABASE IF EXISTS chatdb;
CREATE DATABASE IF NOT EXISTS chatdb;
USE chatdb;
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- Username: Franz, Password: franz123
-- Username: Alexis, Password: alexis123  
-- Username: Roy, Password: charlie123
-- Username: Lenin, Password: lenin123
-- Username: Admin, Password: admin123