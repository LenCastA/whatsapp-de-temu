# Patrones de Diseño Implementados

Este documento describe todos los patrones de diseño implementados en el proyecto y explica por qué son útiles.

---

## 📋 Patrones Implementados

### 1. **Factory Method Pattern** ✅
**Ubicación:** `factory/SocketFactory.java`, `factory/DefaultSocketFactory.java`

**¿Por qué?**
- Encapsula la creación de sockets
- Facilita el testing con mocks
- Permite diferentes implementaciones (SSL, timeouts personalizados, etc.)
- Centraliza la lógica de creación

**Uso:**
```java
SocketFactory factory = new DefaultSocketFactory();
Socket socket = factory.createClientSocket("localhost", 9000);
```

**Beneficios:**
- Separación de responsabilidades
- Testabilidad mejorada
- Flexibilidad para cambiar implementaciones
- Código más mantenible

---

### 2. **Strategy Pattern** ✅
**Ubicación:** `protocol/MessageHandler.java`, `protocol/handlers/*.java`, `protocol/MessageHandlerRegistry.java`

**¿Por qué?**
- Elimina el switch grande en `ClientHandler.processMessage()`
- Facilita agregar nuevos comandos sin modificar código existente
- Cada comando tiene su propia clase (Single Responsibility)
- Mejora la testabilidad

**Estructura:**
- `MessageHandler`: Interfaz Strategy
- `LoginHandler`, `MessageCommandHandler`, etc.: Estrategias concretas
- `MessageHandlerRegistry`: Registry para buscar handlers

**Uso:**
```java
MessageHandlerRegistry registry = new MessageHandlerRegistry();
MessageHandler handler = registry.getHandler("LOGIN");
handler.handle(parts, clientHandler);
```

**Beneficios:**
- Código más limpio y organizado
- Extensibilidad sin modificar código existente
- Fácil de testear cada handler por separado
- Cumple con Open/Closed Principle

---

### 3. **Builder Pattern** ✅
**Ubicación:** `protocol/MessageBuilder.java`

**¿Por qué?**
- Construcción segura de mensajes del protocolo
- Evita errores de formato (ej: olvidar separadores)
- Código más legible
- Validación automática

**Uso:**
```java
// Método fluido
String message = MessageBuilder.create()
    .withType("MSG")
    .withParam("destinatario")
    .withParam("mensaje")
    .build();

// Métodos de conveniencia
String loginMsg = MessageBuilder.buildLogin("user", "pass");
String errorMsg = MessageBuilder.buildError("Error de autenticación");
```

**Beneficios:**
- Previene errores de formato
- Código más legible
- Reutilización de código común
- Validación centralizada

---

### 4. **Repository Pattern** ✅
**Ubicación:** `repository/UserRepository.java`, `repository/DatabaseUserRepository.java`

**¿Por qué?**
- Abstrae el acceso a la base de datos
- Facilita el testing con mocks
- Permite cambiar la implementación (ej: de MySQL a PostgreSQL)
- Centraliza la lógica de acceso a datos

**Estructura:**
- `UserRepository`: Interfaz del repositorio
- `DatabaseUserRepository`: Implementación con MySQL

**Uso:**
```java
UserRepository repository = new DatabaseUserRepository();
boolean isValid = repository.authenticate("user", "pass");
```

**Beneficios:**
- Desacoplamiento de la base de datos
- Testabilidad mejorada
- Flexibilidad para cambiar implementaciones
- Código más mantenible

---

### 5. **Observer Pattern** ✅
**Ubicación:** `observer/ChatObserver.java`, `observer/ChatEvent.java`, `observer/ChatEventPublisher.java`

**¿Por qué?**
- Desacopla componentes que generan eventos de los que los consumen
- Permite agregar nuevos observadores sin modificar código existente
- Útil para logging, estadísticas, notificaciones, etc.

**Estructura:**
- `ChatObserver`: Interfaz Observer
- `ChatEvent`: Clase de evento
- `ChatEventPublisher`: Subject que notifica eventos
- `LoggingObserver`: Implementación de ejemplo

**Uso:**
```java
ChatEventPublisher publisher = new ChatEventPublisher();
publisher.subscribe(new LoggingObserver());
publisher.publishEvent(EventType.USER_CONNECTED, "user", "Usuario conectado");
```

**Beneficios:**
- Desacoplamiento entre componentes
- Extensibilidad sin modificar código existente
- Fácil agregar nuevos tipos de observadores
- Cumple con Open/Closed Principle

---

### 6. **Command Pattern** ✅ (Ya existía)
**Ubicación:** `commands/*.java`

**¿Por qué?**
- Encapsula solicitudes como objetos
- Permite parametrizar objetos con operaciones
- Facilita operaciones de deshacer/rehacer
- Desacopla el invocador de la operación

**Estructura:**
- `Command`: Interfaz del comando
- `ChatCommand`, `FileCommand`, etc.: Comandos concretos
- `MenuCommandInvoker`: Invocador de comandos

---

### 7. **Singleton Pattern** ✅ (Ya existía)
**Ubicación:** `EjecutorSql.java`

**¿Por qué?**
- Asegura una única instancia de una clase
- Útil para recursos compartidos
- Controla el acceso a recursos globales

---

## 🎯 Resumen de Beneficios

### Mantenibilidad
- Código más organizado y fácil de entender
- Separación clara de responsabilidades
- Fácil localizar y modificar funcionalidades

### Extensibilidad
- Agregar nuevas funcionalidades sin modificar código existente
- Cumple con Open/Closed Principle
- Fácil agregar nuevos comandos, handlers, observadores

### Testabilidad
- Interfaces permiten crear mocks fácilmente
- Cada componente puede testearse independientemente
- Factory Pattern facilita inyección de dependencias

### Reutilización
- Componentes reutilizables en diferentes contextos
- Builder Pattern para construir objetos complejos
- Repository Pattern para diferentes tipos de datos

---

## 📝 Próximos Pasos Sugeridos

1. **Integrar Strategy Pattern en ClientHandler**: Modificar `processMessage()` para usar el registry
2. **Usar MessageBuilder**: Reemplazar concatenación de strings por MessageBuilder
3. **Integrar Repository**: Modificar `Database` para usar `UserRepository`
4. **Integrar Observer**: Agregar notificaciones de eventos en `ChatServer` y `ClientHandler`
5. **Template Method Pattern**: Para operaciones de base de datos que siguen el mismo patrón
6. **Adapter Pattern**: Para adaptar diferentes protocolos o formatos de mensajes

---

## 🔗 Referencias

- **Factory Method**: Creación de objetos sin especificar clases exactas
- **Strategy**: Intercambiar algoritmos en tiempo de ejecución
- **Builder**: Construcción paso a paso de objetos complejos
- **Repository**: Abstracción de acceso a datos
- **Observer**: Notificación de cambios a múltiples objetos
- **Command**: Encapsular solicitudes como objetos
- **Singleton**: Una única instancia de una clase

