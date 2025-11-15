# Mejoras de Seguridad Implementadas

Este documento describe las mejoras de seguridad implementadas en el proyecto.

## 🔒 Cambios de Seguridad

### 1. Hash de Contraseñas con BCrypt
- **Antes**: Las contraseñas se almacenaban en texto plano en la base de datos.
- **Ahora**: Todas las contraseñas se hashean usando BCrypt antes de almacenarse.
- **Implementación**: 
  - Clase `PasswordHasher` para hashear y verificar contraseñas
  - Cost factor de 10 (balance entre seguridad y rendimiento)
  - Sal automática incluida en cada hash

### 2. Gestión de Configuración
- **Antes**: Credenciales de base de datos hardcodeadas en el código.
- **Ahora**: Credenciales almacenadas en archivo `config.properties` (no incluido en el repositorio).
- **Implementación**:
  - Clase `ConfigManager` para gestionar configuración
  - Valores por defecto si el archivo no existe
  - El archivo se crea automáticamente en la primera ejecución

### 3. Validación de Entrada
- **Antes**: No había validación de entrada del usuario.
- **Ahora**: Validación completa de todos los inputs.
- **Implementación**:
  - Clase `InputValidator` con validaciones para:
    - Username: 3-50 caracteres, solo alfanuméricos, guiones y guiones bajos
    - Password: 6-128 caracteres, debe contener letras y números
    - Puerto: Validación de rango (1-65535)
    - Host: Validación básica de formato

## 📋 Uso

### Configuración Inicial

1. Al ejecutar la aplicación por primera vez, se creará automáticamente el archivo `config.properties` con valores por defecto.
2. Para cambiar las credenciales de MySQL, usa la opción "Configurar base de datos MySQL" en el menú del servidor.

### Crear Usuarios

Los usuarios ahora deben crearse usando la función de registro que automáticamente hashea las contraseñas:

1. Ve al menú del servidor
2. Selecciona "Registrar nuevo usuario"
3. Ingresa username y password (se validarán automáticamente)
4. La contraseña se hasheará antes de guardarse

### Migración de Usuarios Existentes

Si tienes usuarios existentes con contraseñas en texto plano:

1. **Opción 1**: Eliminar usuarios antiguos y recrearlos usando el registro
2. **Opción 2**: Crear un script de migración que hashee las contraseñas existentes

## ⚠️ Notas Importantes

- **NUNCA** subas el archivo `config.properties` al repositorio
- El archivo está incluido en `.gitignore` por seguridad
- Las contraseñas de los usuarios de prueba deben crearse manualmente después de ejecutar el schema
- BCrypt genera un hash diferente cada vez (debido a la sal), pero la verificación funciona correctamente

## 🔐 Mejores Prácticas

1. Usa contraseñas fuertes (mínimo 6 caracteres, con letras y números)
2. No compartas el archivo `config.properties`
3. Cambia las credenciales por defecto de MySQL
4. Mantén el archivo de configuración en un lugar seguro

## 📝 Archivos Modificados

- `pom.xml`: Agregada dependencia de BCrypt (jbcrypt)
- `Database.java`: Usa BCrypt para hash y verificación
- `Main.java`: Validación de entrada implementada
- `schema.sql`: Actualizado para no incluir contraseñas en texto plano
- Nuevos archivos:
  - `PasswordHasher.java`: Utilidad para hash de contraseñas
  - `ConfigManager.java`: Gestión de configuración
  - `InputValidator.java`: Validación de entrada

