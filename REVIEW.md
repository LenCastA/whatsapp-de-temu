# Informe de revisión de arquitectura y funcionalidades

## Resumen funcional
- El flujo interactivo inicia en `Main`, donde se delega al `MenuController` para escoger entre los modos servidor y cliente. El menú permite configurar la base de datos, registrar usuarios y arrancar cada componente, lo que confirma que la UI de consola actúa como fachada para los servicios de infraestructura.【F:src/main/java/com/mycompany/chat/Main.java†L3-L41】【F:src/main/java/com/mycompany/chat/ui/MenuController.java†L31-L178】
- En modo servidor, `ChatServer` abre sockets de datos y video, valida la conexión MySQL y delega cada conexión entrante a `ClientHandler`, que implementa la autenticación, mensajería privada, transferencia de archivos y canal de video punto a punto.【F:src/main/java/com/mycompany/chat/ChatServer.java†L19-L176】【F:src/main/java/com/mycompany/chat/ClientHandler.java†L47-L285】
- En modo cliente, `ChatClient` crea los dos sockets (datos + video), sincroniza el inicio de sesión mediante un `CountDownLatch`, ofrece un menú por comandos para enviar mensajes, archivos o video, y descarga automáticamente cualquier archivo recibido en la carpeta `downloads`.【F:src/main/java/com/mycompany/chat/ChatClient.java†L83-L508】

## Patrones de diseño ya presentes
- **Command**: el menú principal del cliente se basa en `Command` + `MenuCommandInvoker` para registrar/ejecutar acciones como chat, archivos, video y salida.【F:src/main/java/com/mycompany/chat/ChatClient.java†L321-L369】【F:src/main/java/com/mycompany/chat/commands/Command.java†L3-L19】
- **Strategy/Registry**: `ClientHandler` enruta comandos a través de `MessageHandlerRegistry`, que actualmente incluye handlers para login, logout, mensajes y listado de usuarios.【F:src/main/java/com/mycompany/chat/ClientHandler.java†L83-L117】【F:src/main/java/com/mycompany/chat/protocol/MessageHandlerRegistry.java†L12-L49】
- **Factory Method**: `SocketFactory` abstrae la creación de sockets y permite usar implementaciones alternativas (por ejemplo, en pruebas).【F:src/main/java/com/mycompany/chat/factory/SocketFactory.java†L8-L45】【F:src/main/java/com/mycompany/chat/ChatServer.java†L47-L52】
- **Singleton**: `EjecutorSql` emplea double-checked locking para garantizar una única instancia al ejecutar el script SQL inicial.【F:src/main/java/com/mycompany/chat/EjecutorSql.java†L18-L39】
- **Builder y Observer definidos pero infrautilizados**: existen `MessageBuilder` y `ChatEventPublisher`, aunque todavía no se usan dentro de los flujos críticos.【F:src/main/java/com/mycompany/chat/protocol/MessageBuilder.java†L1-L87】【F:src/main/java/com/mycompany/chat/observer/ChatEventPublisher.java†L8-L60】

## Hallazgos y oportunidades de mejora
1. **Completar el Strategy Pattern para FILE/VIDEO**  
   Actualmente `ClientHandler` detecta manualmente los comandos `FILE` y `VIDEO`, a pesar de que existe un comentario que indica la intención de moverlos a handlers. Esto rompe la extensibilidad del registro de estrategias y complica las pruebas unitarias de cada tipo de mensaje.【F:src/main/java/com/mycompany/chat/ClientHandler.java†L83-L251】  
   *Sugerencia*: crear `FileCommandHandler` y `VideoCommandHandler` que implementen `MessageHandler`, y registrarlos desde `MessageHandlerRegistry`. Así se elimina la lógica condicional del handler principal y se reduce el acoplamiento entre transporte y negocio.

2. **Duplicación de capa de acceso a datos**  
   La aplicación mantiene dos enfoques distintos para interactuar con MySQL: la clase estática `Database`, que maneja autenticación y registro, y `DatabaseUserRepository`, que expone exactamente los mismos métodos pero mediante una interfaz. Los servicios (`DatabaseService`) siguen llamando directamente a `Database`, por lo que el repository nunca se usa y el código de JDBC está duplicado.【F:src/main/java/com/mycompany/chat/Database.java†L1-L86】【F:src/main/java/com/mycompany/chat/repository/DatabaseUserRepository.java†L1-L78】【F:src/main/java/com/mycompany/chat/service/DatabaseService.java†L12-L51】  
   *Sugerencia*: escoger un solo punto de acceso (idealmente la interfaz `UserRepository`), inyectarlo donde corresponda y eliminar las utilidades estáticas. Esto abriría la puerta a pruebas con bases de datos embebidas o repositorios en memoria mediante Dependency Injection.

3. **`MessageBuilder` definido pero no utilizado**  
   A pesar de contar con un Builder para construir mensajes del protocolo, la mayor parte del código continúa concatenando strings manualmente al enviar comandos (por ejemplo, en `sendFileSync` o al confirmar mensajes en el servidor). Esto aumenta el riesgo de errores de formato y dificulta cambios futuros al separador del protocolo.【F:src/main/java/com/mycompany/chat/protocol/MessageBuilder.java†L1-L87】【F:src/main/java/com/mycompany/chat/ChatClient.java†L427-L475】【F:src/main/java/com/mycompany/chat/ClientHandler.java†L213-L240】  
   *Sugerencia*: reemplazar las concatenaciones por llamadas a `MessageBuilder` (por ejemplo `MessageBuilder.buildMessage(...)`, `MessageBuilder.buildOk(...)`) y añadir pruebas automatizadas que validen los mensajes resultantes.

4. **Sincronización frágil entre sockets de datos y video**  
   El servidor acepta primero un socket de datos y luego bloquea esperando el socket de video en el mismo bucle. Si dos clientes se conectan casi simultáneamente, existe el riesgo de que el segundo cliente ocupe el `videoServer.accept()` pendiente y quede emparejado con el socket de datos del primero, generando cruces de video difíciles de depurar.【F:src/main/java/com/mycompany/chat/ChatServer.java†L66-L74】  
   *Sugerencia*: introducir un protocolo de handshake (por ejemplo, enviar un identificador único tras la autenticación y esperar a que el cliente abra el canal de video usando ese ID), o aceptar las conexiones de video en un hilo separado que asocie sockets mediante metadatos compartidos.

5. **Gestión de threads de video fuera del pool**  
   Al iniciar una videollamada, el servidor lanza un `new Thread(this::receiveVideo)` directamente desde `ClientHandler`, ignorando el `ExecutorService` del servidor, mientras que el cliente crea tareas de video sobre el mismo pool usado para enviar mensajes y archivos. Esto puede generar fugas de threads y contención cuando hay múltiples videollamadas simultáneas.【F:src/main/java/com/mycompany/chat/ClientHandler.java†L223-L235】【F:src/main/java/com/mycompany/chat/ChatClient.java†L392-L713】  
   *Sugerencia*: reutilizar el `ExecutorService` (servidor y cliente) para ejecutar los bucles de video y proporcionar un mecanismo explícito de cancelación para liberar recursos (por ejemplo, `Future.cancel(true)` y cierres ordenados de las cámaras).

6. **Campos y dependencias no utilizados**  
   `ChatClient` declara `BufferedReader in` y `PrintWriter out` que nunca se inicializan ni se usan, lo que sugiere deuda técnica tras migrar a `DataInputStream/DataOutputStream`. De igual forma, el paquete `observer` no está enganchado a ningún evento del servidor, desaprovechando el patrón ya implementado.【F:src/main/java/com/mycompany/chat/ChatClient.java†L54-L60】【F:src/main/java/com/mycompany/chat/observer/ChatEventPublisher.java†L8-L60】  
   *Sugerencia*: eliminar los campos muertos o integrarlos nuevamente (por ejemplo, usar `ChatEventPublisher` para registrar auditorías de login/logout) para mantener el código coherente y documentado.

7. **Validaciones inconsistentes y repetidas**  
   Existen dos fuentes de verdad para los límites de usuarios y contraseñas: `Constants` define longitudes mínimas/máximas pero `InputValidator` vuelve a declarar los mismos valores y patrones. Además, las entradas de `MenuController` no reutilizan la sanitización disponible antes de persistir credenciales en `config.properties`. Esto dificulta el mantenimiento y puede causar divergencias si los límites cambian.【F:src/main/java/com/mycompany/chat/util/Constants.java†L13-L41】【F:src/main/java/com/mycompany/chat/security/InputValidator.java†L8-L68】【F:src/main/java/com/mycompany/chat/ui/MenuController.java†L102-L177】  
   *Sugerencia*: centralizar las constantes de validación en `Constants`, exponer métodos reutilizables en `InputValidator` y aplicarlos sistemáticamente cuando se soliciten hosts, puertos o credenciales.

## Posibles extensiones funcionales
- Añadir un comando de **registro desde el cliente** que invoque `Database.registerUser`, reutilizando las validaciones ya existentes, para evitar depender de la consola del servidor.【F:src/main/java/com/mycompany/chat/service/DatabaseService.java†L33-L51】
- Aprovechar el **Observer Pattern** para generar bitácoras o métricas en cada login/logout, lo que facilitaría auditoría y monitoreo en producción.【F:src/main/java/com/mycompany/chat/observer/ChatEventPublisher.java†L8-L60】
- Implementar **test unitarios** para cada `MessageHandler` (login, logout, mensajes) usando una `SocketFactory` mock para validar escenarios de error sin abrir sockets reales.【F:src/main/java/com/mycompany/chat/protocol/MessageHandlerRegistry.java†L12-L49】【F:src/main/java/com/mycompany/chat/factory/SocketFactory.java†L8-L44】

Este documento cubre los principales puntos detectados durante la revisión y debería servir como guía para priorizar mejoras arquitectónicas y funcionales.
