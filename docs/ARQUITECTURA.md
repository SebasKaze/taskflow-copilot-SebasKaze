# Arquitectura de TaskFlow

Este documento introduce a un desarrollador nuevo en la arquitectura del proyecto y explica dónde están las piezas clave.

1) Visión general por capas y paquetes

- Paquete raíz: `com.taskflow`.
- Controller (HTTP): `com.taskflow.controller` — expone la API REST. Ej.: `src/main/java/com/taskflow/controller/TaskController.java` (`TaskController`) y `src/main/java/com/taskflow/controller/ProjectController.java` (`ProjectController`).
- Service (casos de uso / orquestación): `com.taskflow.service` — contiene `TaskService`, `ProjectService`, `JpaUserDetailsService`, etc. Ej.: `src/main/java/com/taskflow/service/TaskService.java` (`TaskService`).
- Repository (persistencia): `com.taskflow.repository` — Spring Data JPA repositories como `TaskRepository`, `ProjectRepository`.
- Model (entidades y reglas de dominio): `com.taskflow.model` — entidades ricas con comportamiento, por ejemplo `src/main/java/com/taskflow/model/Task.java` (`Task`).
- DTOs y mappers: `com.taskflow.dto` y `com.taskflow.mapper`. Los DTOs son `record` (ej. `src/main/java/com/taskflow/dto/TaskRequest.java` `TaskRequest`) y el mapeo manual vive en `src/main/java/com/taskflow/mapper/TaskMapper.java` (`TaskMapper`).
- Seguridad: `com.taskflow.security` y `com.taskflow.config`. Claves: `src/main/java/com/taskflow/security/JwtService.java` (`JwtService`) y `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java` (`JwtAuthenticationFilter`), y la configuración en `src/main/java/com/taskflow/config/SecurityConfig.java` (`SecurityConfig`).
- Advice y manejo de errores: `com.taskflow.advice` con `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java` (`GlobalExceptionHandler`).
- Recursos estáticos: `src/main/resources/static` (UI servida por la misma API).

2) Recorrido completo de POST /projects/{projectId}/tasks

Paso a paso (request -> DB):

- El request HTTP llega a `TaskController.createTask` en `src/main/java/com/taskflow/controller/TaskController.java` (`TaskController`).
  - `@Valid` valida la forma del `TaskRequest` (`src/main/java/com/taskflow/dto/TaskRequest.java`). Si falla, Spring lanza `MethodArgumentNotValidException` y `GlobalExceptionHandler` devuelve 400.
  - El controller comprueba que el proyecto exista llamando a `projectService.buscarPorId(projectId)` (`src/main/java/com/taskflow/service/ProjectService.java`). Si no existe -> `ProjectNotFoundException` -> 404 por el `GlobalExceptionHandler`.
- Si el proyecto existe, el controller llama a `taskService.crear(request, projectId)` (`src/main/java/com/taskflow/service/TaskService.java`).
- `TaskService.crear` orquesta la creación:
  - Usa `TaskMapper.aEntidadNueva(request, projectId)` (`src/main/java/com/taskflow/mapper/TaskMapper.java`) para convertir el DTO en una entidad.
  - `TaskMapper.aEntidadNueva` llama a la factory de dominio `Task.crear(...)` en `src/main/java/com/taskflow/model/Task.java`. Aquí se aplica la regla "`dueDate` no puede estar en el pasado al crear". Si la regla falla, `Task.crear` lanza `TaskValidationException` (checked). `GlobalExceptionHandler` mapea esa excepción a 400.
  - Si la entidad es válida, `TaskService` llama a `taskRepository.save(nueva)` (repositorio Spring Data) y la BD asigna el `id`.
- El controller construye la respuesta `201 Created` con cabecera `Location: /tasks/{id}` y el cuerpo `TaskResponse` (mapeado con `TaskMapper.aResponse`).

Errores y mapeos relevantes:
- Reglas de forma (Bean Validation en DTO) -> 400 por `GlobalExceptionHandler`.
- Reglas de dominio al crear (p.ej. fecha pasada) -> `TaskValidationException` -> 400 por `GlobalExceptionHandler`.
- Reglas de transición de estado (p.ej. "no DONE sin assignee") viven en `Task.setStatus(...)` y, si se violan durante un `PATCH`/cambio de estado, `TaskService` traduce la excepción a `TaskStateException` -> 422 por `GlobalExceptionHandler`.

3) Dónde viven las reglas de negocio

- Reglas de invariantes y transiciones (lógica del dominio) residen en la entidad `Task` (`src/main/java/com/taskflow/model/Task.java`): factory `Task.crear(...)` (regla temporal al crear) y `setStatus(...)` (reglas de transición de estado).
- Reglas de orquestación (flujos, consultas, persistencia, integridad entre entidades) residen en los services (`TaskService`, `ProjectService`). Los services NO duplican reglas de dominio; las invocan.
- Validaciones de formato y contrato (frontera) están en los DTOs (`record` con anotaciones Bean Validation, p.ej. `TaskRequest`).

4) Seguridad con JWT

- Emisión: el `JwtService` (`src/main/java/com/taskflow/security/JwtService.java`) genera tokens firmados (HS256) que contienen `sub` = username, `role` (claim informativo), y `iat`/`exp`.
- Autenticación en cada request: `JwtAuthenticationFilter` (`src/main/java/com/taskflow/security/JwtAuthenticationFilter.java`) examina el header `Authorization: Bearer <token>` antes del DispatcherServlet. Verifica firma y expiración; si es válido carga `UserDetails` desde BD (`JpaUserDetailsService`) y coloca una `Authentication` en el `SecurityContext`.
  - Si el token es corrupto o expirado, el filtro responde `401` directamente (el `GlobalExceptionHandler` NO ve las excepciones lanzadas antes del DispatcherServlet).
- Configuración: `SecurityConfig` registra el filtro y expone rutas públicas (`/auth/**`, `/info`, consola H2, swagger, recursos estáticos). Todo lo demás requiere token.
- Autorización: se usan roles (`ROLE_ADMIN`, `ROLE_USER`) y `@PreAuthorize` para checks en controladores; ejemplo: `@PreAuthorize("hasRole('ADMIN') or @projectSecurity.esOwner(#id, authentication.name)")` en `ProjectController` para borrar un proyecto. El bean `ProjectSecurity` contiene la lógica data-driven de "esOwner".
- En controllers que necesitan el owner del recurso, el username se obtiene de la `Authentication` (p.ej. `authentication.getName()`), y `ProjectService` resuelve a `ownerId` usando `UserRepository`.

5) Organización de tests

- Tests unitarios (sin Spring) para clases de dominio y utilidades: ubicados en `src/test/java/.../unit` (suelen usar JUnit 5 + Mockito).
- Tests slice: `@WebMvcTest` y `@DataJpaTest` están en `src/test/java/.../slice`. Ejemplo: `src/test/java/com/taskflow/slice/TaskControllerTest.java` (`TaskControllerTest`).
- Tests de integración: `@SpringBootTest` con perfil `test` en `src/test/java/.../integration`. Ejemplo: `src/test/java/com/taskflow/integration/SecurityRulesTest.java`.
- Nota operacional (convención del repo): los `*IT.java` usan Testcontainers y no corren con `mvn test` salvo que se active `-Ddocker.tests=true`.

6) Puntos prácticos y convenciones rápidas

- DTOs son `record` y se validan con `@Valid` en el controller.
- El controller nunca devuelve entidades; siempre `TaskMapper` y `ProjectMapper` producen DTOs de respuesta, p. ej. `src/main/java/com/taskflow/dto/TaskResponse.java` (`TaskResponse`) y `src/main/java/com/taskflow/dto/ProjectResponse.java` (`ProjectResponse`).
- No se crea un handler genérico de `Exception` (evitar atrapar silenciosamente errores de Spring): `GlobalExceptionHandler` maneja tipos concretos.
- DataSeeder: `src/main/java/com/taskflow/config/DataSeeder.java` alimenta datos de ejemplo con perfil `h2`.
- Comandos útiles (Windows/PowerShell):
  - `mvn -q test` — suite normal
  - `mvn spring-boot:run "-Dspring-boot.run.profiles=h2"` — app con H2 y datos de `DataSeeder`

Lecturas recomendadas dentro del repo (rutas):
- `src/main/java/com/taskflow/model/Task.java` — reglas de dominio y fábrica.
- `src/main/java/com/taskflow/mapper/TaskMapper.java` — cómo convertir `TaskRequest` ↔ `Task` ↔ `TaskResponse`.
- `src/main/java/com/taskflow/service/TaskService.java` — orquestación de casos de uso de tareas.
- `src/main/java/com/taskflow/controller/TaskController.java` — endpoints de tareas (POST/PUT/PATCH/DELETE).
- `src/main/java/com/taskflow/security/JwtService.java` y `src/main/java/com/taskflow/security/JwtAuthenticationFilter.java` — flujo JWT.
- `src/main/java/com/taskflow/advice/GlobalExceptionHandler.java` — mapeo de excepciones a responses HTTP.

Si quieres, puedo añadir un diagrama de secuencia (texto o imagen) para `POST /projects/{projectId}/tasks` o convertir este documento en un README más corto para nuevos hires.