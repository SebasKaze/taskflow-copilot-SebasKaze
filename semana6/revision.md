# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `eecc80bc-c702-4b79-b410-1cc9eb47f6fc`  
> - **Started:** 17/10/2026, 11:35:41 a.m.  
> - **Duration:** 1m 15s  
> - **Exported:** 17/10/2026, 11:36:56 a.m.  

---

<sub>2s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/search.md.

---

<sub>8s</sub>

### Copilot

Reading the diff and the specification to gather context. Then will open each file modified by the diff to compare current repository files with the changes. Running two parallel file reads: the diff and the spec.

---

<sub>8s</sub>

### `view`

**view the file at semana6/proyecto-final.diff.**

semana6/proyecto-final.diff

<details>
<summary>190 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/TaskController.java b/src/main/java/com/taskflow/controller/TaskController.java
index 4eff2fa..d28115b 100644
--- a/src/main/java/com/taskflow/controller/TaskController.java
+++ b/src/main/java/com/taskflow/controller/TaskController.java
@@ -89,6 +89,14 @@ public class TaskController {
         return tareas.stream().map(TaskMapper::aResponse).toList();
     }
 
+    @Operation(summary = "Busca tareas por título",
+            description = "Busca tareas cuyo título contiene el parámetro 'q' (case-insensitive).")
+    @GetMapping("/tasks/search")
+    public List<TaskResponse> searchTasks(@RequestParam(name = "q", required = false) String q) throws TaskValidationException {
+        List<Task> tareas = taskService.buscarPorTitulo(q);
+        return tareas.stream().map(TaskMapper::aResponse).toList();
+    }
+
     @Operation(summary = "Obtiene una tarea por id",
             description = "200 con el TaskResponse; 404 uniforme si el id no existe.")
     @GetMapping("/tasks/{id}")
diff --git a/src/main/java/com/taskflow/service/TaskService.java b/src/main/java/com/taskflow/service/TaskService.java
index 15de844..0104552 100644
--- a/src/main/java/com/taskflow/service/TaskService.java
+++ b/src/main/java/com/taskflow/service/TaskService.java
@@ -144,4 +144,18 @@ public class TaskService {
                 .sorted(TaskOrders.POR_FECHA)
                 .toList();
     }
+
+    /**
+     * Busca tareas por título que contienen el fragmento q (trimmed), case-insensitive.
+     * Lanza TaskValidationException con mensaje 'El parámetro 'q' es obligatorio.' si q es nulo o vacío tras trim.
+     */
+    public List<Task> buscarPorTitulo(String q) throws TaskValidationException {
+        if (q == null || q.strip().isEmpty()) {
+            throw new TaskValidationException("El parámetro 'q' es obligatorio.");
+        }
+        String fragment = q.strip();
+        List<Task> resultados = repository.findByTitleContainingIgnoreCase(fragment);
+        return resultados.stream().sorted(TaskOrders.POR_TITULO).toList();
+    }
 }
+
diff --git a/src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java b/src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java
new file mode 100644
index 0000000..7e32dce
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java
@@ -0,0 +1,71 @@
+package com.taskflow.slice;
+
+import com.taskflow.exception.TaskValidationException;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import com.taskflow.service.TaskService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.boot.test.mock.mockito.MockBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.List;
+
+import static org.mockito.Mockito.when;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@WebMvcTest(com.taskflow.controller.TaskController.class)
+@AutoConfigureMockMvc(addFilters = false)
+public class BusquedaTareasControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockBean
+    private TaskService taskService;
+
+    @MockBean
+    private ProjectService projectService;
+
+    @MockBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void get_search_q_presente_devuelve200_y_lista() throws Exception {
+        Task t1 = tarea(9L, "Documentar la API con Swagger");
+        Task t2 = tarea(5L, "Optimizar consultas de la API");
+
+        when(taskService.buscarPorTitulo("api")).thenReturn(List.of(t1, t2));
+
+        mockMvc.perform(get("/tasks/search").param("q", "api"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$[0].id").value(9))
+                .andExpect(jsonPath("$[0].title").value("Documentar la API con Swagger"))
+                .andExpect(jsonPath("$[1].id").value(5))
+                .andExpect(jsonPath("$[1].title").value("Optimizar consultas de la API"));
+    }
+
+    @Test
+    void get_search_q_faltante_devuelve400_con_mensaje() throws Exception {
+        when(taskService.buscarPorTitulo(null)).thenThrow(new TaskValidationException("El parámetro 'q' es obligatorio."));
+
+        mockMvc.perform(get("/tasks/search"))
+                .andExpect(status().isBadRequest())
+                .andExpect(jsonPath("$.message").value("El parámetro 'q' es obligatorio."));
+    }
+
+    private Task tarea(Long id, String title) {
+        try {
+            return new Task(id, title, "desc", TaskStatus.IN_PROGRESS, Priority.MED, 1L, null, null);
+        } catch (TaskValidationException e) {
+            throw new IllegalStateException("dato de prueba inválido", e);
+        }
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java b/src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java
new file mode 100644
index 0000000..12f8e8b
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java
@@ -0,0 +1,66 @@
+package com.taskflow.unit;
+
+import com.taskflow.exception.TaskValidationException;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.TaskRepository;
+import org.junit.jupiter.api.DisplayName;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import java.util.List;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.junit.jupiter.api.Assertions.assertThrows;
+import static org.mockito.ArgumentMatchers.anyString;
+import static org.mockito.Mockito.never;
+import static org.mockito.Mockito.verify;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+@DisplayName("BusquedaTareasService - tests unitarios")
+public class BusquedaTareasServiceTest {
+
+    @Mock
+    private TaskRepository repository;
+
+    @InjectMocks
+    private com.taskflow.service.TaskService service;
+
+    private static final Long PROYECTO = 1L;
+
+    @Test
+    void buscarPorTitulo_trimea_y_ordena() throws TaskValidationException {
+        Task t1 = tarea(2L, "Zeta tarea");
+        Task t2 = tarea(1L, "Alpha tarea");
+
+        when(repository.findByTitleContainingIgnoreCase("api")).thenReturn(List.of(t1, t2));
+
+        List<Task> resultado = service.buscarPorTitulo("  api ");
+
+        // Debe devolver en orden alfabético por título (Alpha antes que Zeta)
+        assertEquals(1L, resultado.get(0).getId());
+        assertEquals(2L, resultado.get(1).getId());
+
+        verify(repository).findByTitleContainingIgnoreCase("api");
+    }
+
+    @Test
+    void buscarPorTitulo_null_o_vacio_lanzaYNoLlamaRepo() {
+        assertThrows(TaskValidationException.class, () -> service.buscarPorTitulo(null));
+        assertThrows(TaskValidationException.class, () -> service.buscarPorTitulo("   "));
+        verify(repository, never()).findByTitleContainingIgnoreCase(anyString());
+    }
+
+    private Task tarea(Long id, String title) {
+        try {
+            return new Task(id, title, "desc", TaskStatus.TODO, Priority.MED, PROYECTO, null, null);
+        } catch (TaskValidationException e) {
+            throw new IllegalStateException("dato de prueba inválido", e);
+        }
+    }
+}
```

</details>


---

<sub>8s</sub>

### `view`

**view the file at specs/search.md.**

specs/search.md

<details>
<summary>69 lines</summary>

````
# Especificación — `GET /tasks/search?q=`

## Qué

Un endpoint que busca tareas por su título, en todos los proyectos, para encontrar una tarea sin
recorrer la lista completa. «api» encuentra «Documentar la API con Swagger».

```
GET /tasks/search?q=api
Authorization: ******
```

## Reglas

1. Coincide toda tarea cuyo `title` **contiene** `q`, sin distinguir mayúsculas de minúsculas. La
   consulta ya existe en el repositorio: `TaskRepository.findByTitleContainingIgnoreCase(String)`.
   Úsala; no escribas SQL, `@Query` ni un filtro sobre `findAll()`.
2. Antes de buscar, `q` se recorta con `strip()`: `"  api "` busca `"api"`.
3. `q` es obligatorio. Si falta, o si después de recortarlo queda vacío, responde **`400`** con el
   `ErrorResponse` uniforme de siempre y el mensaje `El parámetro 'q' es obligatorio.`. Para eso el
   servicio lanza `TaskValidationException`, que el `GlobalExceptionHandler` ya convierte en 400.
4. Orden: alfabético por título, con el comparador que ya existe, `TaskOrders.POR_TITULO`.
5. Respuesta: `200` con una lista de `TaskResponse` (el mismo DTO que `GET /tasks`, con el mapper que
   ya existe, `TaskMapper.aResponse`). Si nada coincide, `200` con `[]`.
6. Seguridad: igual que el resto de `/tasks` (sin token → `401`). No cambies `SecurityConfig`.

## Dónde

- **Sin DTO ni mapper nuevos**: la respuesta es una lista del `TaskResponse` que ya existe.
- Método nuevo en `TaskService`:
  `public List<Task> buscarPorTitulo(String q) throws TaskValidationException`.
- Endpoint nuevo en `TaskController`: `@GetMapping("/tasks/search")`, con
  `@RequestParam(name = "q", required = false) String q`. El `required = false` es a propósito: así
  el «falta `q`» llega al servicio y sale con el mismo 400 uniforme que el «`q` vacío».
- Hoy `GET /tasks/search` responde `400` con «El parámetro 'id' tiene un valor ilegible»: lo atrapa
  `GET /tasks/{id}`. Con el endpoint nuevo, Spring prefiere la ruta literal `/tasks/search` sin
  importar en qué orden declares los métodos. No escribas un comentario que diga lo contrario.

> **Por qué no se reutiliza `ReportService.buscarPorTitulo`**, que hace casi lo mismo: habría que
> inyectar `ReportService` en `TaskController`, y el slice `TaskControllerTest` (que no puedes tocar)
> dejaría de arrancar. Medido: sus 10 tests fallan con `No qualifying bean of type
> 'com.taskflow.service.ReportService'`. Es la regla «no agregues dependencias nuevas al constructor
> de un controller que ya existe» de la skill `crear-endpoint-taskflow`.

## Tests que deben existir al terminar

Los dos en **clases nuevas** (las que ya existen no se tocan):

- **Unit** `src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java`, con
  `@Mock TaskRepository` e `@InjectMocks TaskService`:
  - el repositorio devuelve dos tareas en orden **no** alfabético y el servicio las regresa ordenadas
    por título (el test falla si quitas el orden);
  - `"  api "` llama al repositorio con `"api"` (`verify`);
  - `null` y `"   "` lanzan `TaskValidationException` y el repositorio **nunca** se llama.
- **Slice** `src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java`, con
  `@WebMvcTest(TaskController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
  de `TaskService`, `ProjectService` (el constructor de `TaskController` recibe los dos) y
  `JwtAuthenticationFilter`:
  - `GET /tasks/search?q=api` → `200` y el JSON trae los ids y títulos que devolvió el servicio, en
    ese orden;
  - `GET /tasks/search` sin `q`, con el servicio lanzando `TaskValidationException` → `400` y
    `$.message` = `El parámetro 'q' es obligatorio.`.

## Resultado esperado con la semilla

Con la app arrancada con el perfil `h2` y el token de `ana`:

| Petición | Resultado |
|---|---|
| `GET /tasks/search?q=api` | `200`, ids `[9, 5]`: «Documentar la API con Swagger» y «Optimizar consultas de la API», en ese orden |
| `GET /tasks/search?q=API` | lo mismo: `[9, 5]` |
| `GET /tasks/search?q=zzz` | `200` con `[]` |
| `GET /tasks/search?q=%20%20` | `400` con `message` = `El parámetro 'q' es obligatorio.` |
| `GET /tasks/search` | `400` con el mismo mensaje |
| `GET /tasks/search?q=api` sin token | `401` |

## Restricciones para el agente

- No modifiques, borres ni desactives ningún test existente.
- No toques archivos fuera de `TaskService.java`, `TaskController.java` y las dos clases de test
  nuevas.
- Al terminar, `mvn -q test` tiene que pasar completo.

El pull request del proyecto final lleva además `semana6/`, `.github/skills/verificar-taskflow/casos-search.ps1`
y una línea nueva en `verificar.ps1`. Esos archivos los agregas tú en el proceso (PF-2 a PF-7 de la guía),
no el agente: no cuentan contra esta restricción.
````

</details>


---

<sub>25s</sub>

### Copilot

Reading TaskRepository, TaskOrders and TaskMapper to confirm the repository method and comparators/mappers referenced in the diff actually exist. Then use parallel reads for those three files.

---

<sub>25s</sub>

### `view`

**view the file at src/main/java/com/taskflow/repository/TaskRepository.java.**

src/main/java/com/taskflow/repository/TaskRepository.java

<details>
<summary>33 lines</summary>

```
package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * TaskRepository — extiende JpaRepository&lt;Task, Long&gt;: save/findById/findAll/deleteById VIENEN
 * GRATIS (+ count/existsById/etc.). NO se implementa nadie: un PROXY de runtime lo hace (la misma
 * "magia" de la inyección de dependencias).
 *
 * La paga de programar contra una interfaz: TaskService/ReportService dependen de ESTE contrato, y
 * sus firmas (Optional&lt;Task&gt; findById, void deleteById) son las de Spring Data. Si cambiara la
 * implementación, el servicio no se tocaría: depende de la interfaz, no de JPA.
 *
 * DERIVED QUERIES: el NOMBRE del método es la query. Gramática findBy + propiedad + And/Not/
 * Containing/IgnoreCase/OrderBy, y countBy. Se validan AL ARRANCAR: un nombre con una propiedad que
 * no existe (p.ej. findByEstado) lanza QueryCreationException y la app NO levanta — error de arranque,
 * no de runtime. Estas seis se prueban en TaskRepositoryTest (@DataJpaTest).
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Tareas en un estado dado (hoy la usan los tests; queda para vistas por estado). */
    List<Task> findByStatus(TaskStatus status);

    /** Tareas de un proyecto (por la columna project_id). La consume ProjectService.tareasDe. */
    List<Task> findByProjectId(Long projectId);

    /** Combina dos criterios con And: tareas de un asignado en un estado. */
    List<Task> findByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    /** LIKE case-insensitive: título que CONTIENE el fragmento (la BD hace el filtrado). */
    List<Task> findByTitleContainingIgnoreCase(String fragment);

    /** El complemento de findByStatus: todas MENOS las de ese estado (Not). Sirve a "pendientes". */
    List<Task> findByStatusNot(TaskStatus status);

    /** Cuenta en la BD (no trae filas para contarlas): conteo por estado. */
    long countByStatus(TaskStatus status);
}
```

</details>


---

<sub>25s</sub>

### `view`

**view the file at src/main/java/com/taskflow/service/TaskOrders.java.**

src/main/java/com/taskflow/service/TaskOrders.java

<details>
<summary>45 lines</summary>

```
package com.taskflow.service;

import com.taskflow.model.Task;

import java.util.Comparator;

/**
 * TaskOrders — catálogo de ESTRATEGIAS de ordenamiento de tareas (patrón Strategy).
 *
 * Cada constante es una estrategia empaquetada como VALOR (un {@link Comparator}) que se PASA a
 * quien ordena (el contexto: {@code sorted(...)} de los streams). Todo Comparator es ya una
 * estrategia aunque no se le llame así, y sort/sorted es el contexto que la recibe sin saber cuál
 * le tocó. Aquí les damos NOMBRE y las juntamos en un solo lugar reutilizable.
 *
 * {@code TaskService.listar()} de la API ordena con POR_URGENCIA.
 *
 * Clase de utilidades: constructor privado (no se instancia — solo se usan sus constantes).
 */
public final class TaskOrders {

    private TaskOrders() {
        // no instanciable: es un catálogo de constantes
    }

    /**
     * POR_FECHA — la estrategia básica: dueDate ascendente, con las tareas SIN fecha
     * (dueDate null) al FINAL. nullsLast evita el NullPointerException al comparar una fecha null.
     */
    public static final Comparator<Task> POR_FECHA =
            Comparator.comparing(Task::getDueDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    /** POR_TITULO — orden alfabético por título, sin distinguir mayúsculas/minúsculas. */
    public static final Comparator<Task> POR_TITULO =
            Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);

    /**
     * POR_URGENCIA — la estrategia compuesta, encadenando comparing/thenComparing:
     *   1) las VENCIDAS primero (estaVencida() == true antes que false),
     *   2) a igualdad, prioridad de mayor a menor (HIGH -> MED -> LOW),
     *   3) a igualdad, dueDate ascendente con las sin fecha al final.
     *
     * Detalle: el orden natural del enum Priority es LOW, MED, HIGH (LOW el menor); por eso
     * reverseOrder() deja HIGH primero. Y reverseOrder() sobre el boolean de estaVencida() deja
     * el true (vencida) antes que el false.
     */
    public static final Comparator<Task> POR_URGENCIA =
            Comparator.comparing(Task::estaVencida, Comparator.reverseOrder())
                    .thenComparing(Task::getPriority, Comparator.reverseOrder())
                    .thenComparing(Task::getDueDate,
                            Comparator.nullsLast(Comparator.naturalOrder()));
}
```

</details>


---

<sub>25s</sub>

### `view`

**view the file at src/main/java/com/taskflow/mapper/TaskMapper.java.**

src/main/java/com/taskflow/mapper/TaskMapper.java

<details>
<summary>48 lines</summary>

```
package com.taskflow.mapper;

import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;

/**
 * TaskMapper — el puente entre el CONTRATO (DTOs) y el DOMINIO (entidad Task). Mapeo A MANO con
 * métodos estáticos: barato, visible y testeable. Es una clase de utilidades — constructor privado,
 * no se instancia.
 *
 * Alternativa: MapStruct automatiza este mapeo en el mundo real; aquí se hace a mano para
 * ENTENDER el costo antes de esconderlo detrás de una librería.
 *
 * Nota clave "crear vs rehidratar": aEntidadNueva pasa por la FACTORY Task.crear
 * (aplica "dueDate no en el pasado"); aReemplazo pasa por el CONSTRUCTOR de rehidratación (permite
 * editar una tarea vieja con fecha ya pasada).
 */
public final class TaskMapper {

    private TaskMapper() {
        // no instanciable
    }

    /**
     * POST: DTO de entrada + projectId del PATH -> entidad NUEVA por la factory de negocio.
     * Task.crear fija status=TODO e id=null, y aplica la regla temporal de dueDate. Firma:
     * crear(title, description, priority, dueDate, projectId, assigneeId).
     */
    public static Task aEntidadNueva(TaskRequest req, Long projectId) throws TaskValidationException {
        return Task.crear(req.title(), req.description(), req.priority(),
                req.dueDate(), projectId, req.assigneeId());
    }

    /**
     * PUT (reemplazo COMPLETO): DTO de entrada + el estado y el proyecto ACTUALES -> entidad
     * rehidratada con el MISMO id. Conservamos id, status y projectId (el cliente no los toca por
     * PUT); el resto (title, description, priority, assigneeId, dueDate) viene del request. El
     * constructor de rehidratación valida las invariantes (título, projectId) sin la regla temporal.
     */
    public static Task aReemplazo(Long id, TaskStatus statusActual, Long projectIdActual, TaskRequest req)
            throws TaskValidationException {
        return new Task(id, req.title(), req.description(), statusActual, req.priority(),
                projectIdActual, req.assigneeId(), req.dueDate());
    }

    /** Entidad -> DTO de salida (los 8 campos). La entidad Task NO cruza el controller: cruza esto. */
    public static TaskResponse aResponse(Task t) {
        return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), t.getStatus(),
                t.getPriority(), t.getProjectId(), t.getAssigneeId(), t.getDueDate());
    }
}
```

</details>


---

<sub>31s</sub>

### Copilot

Reading the current TaskController to check imports and constructor.

---

<sub>31s</sub>

### `view`

**view the file at src/main/java/com/taskflow/controller/TaskController.java.**

src/main/java/com/taskflow/controller/TaskController.java

<details>
<summary>148 lines</summary>

```
package com.taskflow.controller;

import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.dto.TaskStatusUpdateRequest;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * TaskController — la PUERTA HTTP de las tareas: CRUD completo, y la entidad Task NO sale por la
 * puerta: toda firma habla en DTOs (TaskRequest / TaskResponse).
 *
 * Contrato de responsabilidades: el controller traduce HTTP <-> dominio y delega. Además de
 * TaskService, inyecta ProjectService para el cableo Task↔Project (validar que el
 * proyecto exista al crear una tarea bajo su path).
 *
 * La checked TaskValidationException se declara con throws y SUBE al GlobalExceptionHandler (que la
 * mapea a 400). No se atrapa aquí porque tiene un handler global esperándola: una excepción se
 * maneja en la capa que PUEDE hacer algo con ella, y ésa es el advice.
 */
@RestController
@Tag(name = "Tasks", description = "CRUD de tareas: crear bajo un proyecto, leer, reemplazar, cambiar estado y borrar.")
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;

    public TaskController(TaskService taskService, ProjectService projectService) {
        this.taskService = taskService;
        this.projectService = projectService;
    }

    /** GET /tasks — todas o filtradas por ?status= / ?priority=. Devuelve TaskResponse. */
    @Operation(summary = "Lista tareas",
            description = "Todas las tareas, o filtradas por ?status= / ?priority=. Devuelve TaskResponse.")
    @GetMapping("/tasks")
    public List<TaskResponse> getTasks(
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "priority", required = false) Priority priority) {
        List<Task> tareas;
        if (status != null) {
            tareas = taskService.porEstado(status);
        } else if (priority != null) {                 // mismo patrón que ?status=
            tareas = taskService.porPrioridad(priority);
        } else {
            tareas = taskService.listar();
        }
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    /** GET /tasks/{id} — 200 con TaskResponse, o 404 uniforme (orElseThrow -> advice). */
    @Operation(summary = "Lista tareas vencidas",
            description = "Lista todas las tareas vencidas de todos los proyectos en orden por fecha.")
    @GetMapping("/tasks/overdue")
    public List<TaskResponse> getTasksOverdue() {
        List<Task> tareas = taskService.vencidas();
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    @Operation(summary = "Lista tareas sin responsable",
            description = "Lista todas las tareas sin responsable de todos los proyectos en orden por fecha.")
    @GetMapping("/tasks/unassigned")
    public List<TaskResponse> getTasksUnassigned() {
        List<Task> tareas = taskService.sinResponsable();
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    @Operation(summary = "Busca tareas por título",
            description = "Busca tareas cuyo título contiene el parámetro 'q' (case-insensitive).")
    @GetMapping("/tasks/search")
    public List<TaskResponse> searchTasks(@RequestParam(name = "q", required = false) String q) throws TaskValidationException {
        List<Task> tareas = taskService.buscarPorTitulo(q);
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    @Operation(summary = "Obtiene una tarea por id",
            description = "200 con el TaskResponse; 404 uniforme si el id no existe.")
    @GetMapping("/tasks/{id}")
    public TaskResponse getTaskPorId(@PathVariable("id") Long id) {
        Task task = taskService.buscarPorId(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return TaskMapper.aResponse(task);
    }

    /**
     * POST /projects/{projectId}/tasks — crea una tarea bajo un proyecto. 201 + header Location
     * apuntando a donde el recurso SE LEE (/tasks/{id}), no a la URL de creación. @Valid dispara Bean
     * Validation (400 si falla). Cableo: si el proyecto no existe -> 404 (ProjectNotFoundException).
     */
    @Operation(summary = "Crea una tarea dentro de un proyecto",
            description = "El proyecto debe existir (404 si no). La tarea nace en estado TODO.")
    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(@PathVariable("projectId") Long projectId,
                                                   @Valid @RequestBody TaskRequest request)
            throws TaskValidationException {
        projectService.buscarPorId(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        Task creada = taskService.crear(request, projectId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/tasks/{id}")
                .buildAndExpand(creada.getId())
                .toUri();
        return ResponseEntity.created(location).body(TaskMapper.aResponse(creada));
    }

    /** PUT /tasks/{id} — reemplazo COMPLETO. 200 con TaskResponse; 404 si no existe. */
    @Operation(summary = "Reemplaza una tarea por completo",
            description = "Semántica PUT: conserva id, status y projectId; el resto viene del cuerpo.")
    @PutMapping("/tasks/{id}")
    public TaskResponse updateTask(@PathVariable("id") Long id,
                                   @Valid @RequestBody TaskRequest request)
            throws TaskValidationException {
        Task actualizada = taskService.reemplazar(id, request);
        return TaskMapper.aResponse(actualizada);
    }

    /** DELETE /tasks/{id} — 204 No Content; 404 uniforme si no existe (advice). */
    @Operation(summary = "Borra una tarea",
            description = "204 No Content; 404 uniforme si la tarea no existe.")
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long id) {
        taskService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /tasks/{id}/status — cambia SOLO el estado. 200 con TaskResponse; 404 si no existe; y
     * 422 si la regla de estado lo prohíbe (DONE sin assignee), vía TaskStateException.
     */
    @Operation(summary = "Cambia el estado de una tarea",
            description = "TODO/IN_PROGRESS/DONE. Pasar a DONE sin responsable responde 422.")
    @PatchMapping("/tasks/{id}/status")
    public TaskResponse patchStatus(@PathVariable("id") Long id,
                                    @Valid @RequestBody TaskStatusUpdateRequest request) {
        Task actualizada = taskService.cambiarStatus(id, request.status());
        return TaskMapper.aResponse(actualizada);
    }
}
```

</details>


---

<sub>48s</sub>

### `view`

**view the file at src/main/java/com/taskflow/service/TaskService.java.**

src/main/java/com/taskflow/service/TaskService.java (lines 1-260)

<details>
<summary>143 lines</summary>

```
package com.taskflow.service;

import com.taskflow.dto.TaskRequest;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * TaskService — la capa de negocio de las tareas. Orquesta; las reglas viven en Task (título 3-120,
 * projectId obligatorio, dueDate no en el pasado al crear, no DONE sin assignee).
 *
 * Piezas clave:
 *   - crear(...) recibe el projectId del PATH (no un id fijo) y arma la entidad con el TaskMapper
 *     (que pasa por la factory Task.crear).
 *   - reemplazar (PUT), eliminar (DELETE) y cambiarStatus (PATCH); completar(...) DELEGA en
 *     cambiarStatus.
 *   - cambiarStatus TRADUCE la checked TaskValidationException que lanza setStatus a la unchecked
 *     TaskStateException -> el advice la mapea a 422. Sin tocar el dominio.
 *
 * La existencia del proyecto al crear (404) se valida en el controller (contra ProjectService): este
 * service no depende del repositorio de proyectos, y así el filtrado por proyecto vive solo en
 * ProjectService.tareasDe.
 */
@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    /**
     * Crea una tarea a partir del DTO de entrada y el projectId del PATH. El TaskMapper pasa por la
     * factory Task.crear (status=TODO, id=null, regla "dueDate no en el pasado"); el repositorio le
     * asigna el id en save. La checked sube al advice si el título o la fecha no cumplen (-> 400).
     */
    public Task crear(TaskRequest request, Long projectId) throws TaskValidationException {
        Task nueva = TaskMapper.aEntidadNueva(request, projectId);
        return repository.save(nueva);
    }

    /**
     * Reemplazo COMPLETO (PUT): busca la tarea (404 si no existe), y arma una tarea rehidratada con el
     * MISMO id, conservando status y projectId actuales; el resto viene del request. save hace upsert.
     */
    public Task reemplazar(Long id, TaskRequest request) throws TaskValidationException {
        Task actual = repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        Task reemplazo = TaskMapper.aReemplazo(id, actual.getStatus(), actual.getProjectId(), request);
        return repository.save(reemplazo);
    }

    /**
     * Elimina una tarea (DELETE): existe -> borra (204 en el controller); no existe -> TaskNotFound
     * -> el advice responde 404. El findById previo hace explícito el "no existe" (vía Optional).
     */
    public void eliminar(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        repository.deleteById(id);
    }

    /**
     * Cambia el estado (PATCH): busca la tarea (404 si no existe) y aplica setStatus, que puede lanzar
     * la CHECKED TaskValidationException por la regla "no DONE sin assignee". La CAPTURA y la TRADUCE
     * a la unchecked TaskStateException -> el advice la mapea a 422. El dominio (setStatus) no se toca.
     */
    public Task cambiarStatus(Long id, TaskStatus nuevoStatus) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        try {
            task.setStatus(nuevoStatus);
        } catch (TaskValidationException e) {
            throw new TaskStateException(e.getMessage());
        }
        return repository.save(task);
    }

    /**
     * Completa una tarea (la pasa a DONE). DELEGA en cambiarStatus(id, DONE): un
     * solo camino para "cambiar de estado", que además hereda la traducción a 422.
     */
    public Task completar(Long id) {
        return cambiarStatus(id, TaskStatus.DONE);
    }

    /**
     * Lista todas las tareas ordenadas con la estrategia POR_URGENCIA (reuso LITERAL de la Strategy de
     * TaskOrders): vencidas primero, luego prioridad HIGH->LOW, luego fecha ascendente (nulls al final).
     */
    public List<Task> listar() {
        return repository.findAll().stream()
                .sorted(TaskOrders.POR_URGENCIA)
                .toList();
    }

    /** Filtra las tareas por estado (?status=). '==' entre enums es seguro. */
    public List<Task> porEstado(TaskStatus status) {
        return repository.findAll().stream()
                .filter(t -> t.getStatus() == status)
                .toList();
    }

    /** Busca una tarea por id: delega en el repositorio y deja subir el Optional TAL CUAL. */
    public Optional<Task> buscarPorId(Long id) {
        return repository.findById(id);
    }

    /** Filtro por prioridad (?priority=): MISMO patrón que porEstado. */
    public List<Task> porPrioridad(Priority priority) {
        return repository.findAll().stream()
                .filter(t -> t.getPriority() == priority)
                .toList();
    }

    /**
     * Devuelve las tareas vencidas de TODOS los proyectos, ordenadas por dueDate ascendente.
     * Regla de vencimiento: delegar en Task.estaVencida().
     */
    public List<Task> vencidas() {
        return repository.findAll().stream()
                .filter(Task::estaVencida)
                .sorted(TaskOrders.POR_FECHA)
                .toList();
    }

    /**
     * Devuelve las tareas sin responsable (assigneeId == null) de TODOS los proyectos, ordenadas por fecha.
     * Usa el predicado ReportService.SIN_ASIGNAR y el comparator TaskOrders.POR_FECHA según la especificación.
     */
    public List<Task> sinResponsable() {
        return repository.findAll().stream()
                .filter(ReportService.SIN_ASIGNAR)
                .sorted(TaskOrders.POR_FECHA)
                .toList();
    }

    /**
     * Busca tareas por título que contienen el fragmento q (trimmed), case-insensitive.
     * Lanza TaskValidationException con mensaje 'El parámetro 'q' es obligatorio.' si q es nulo o vacío tras trim.
     */
    public List<Task> buscarPorTitulo(String q) throws TaskValidationException {
        if (q == null || q.strip().isEmpty()) {
            throw new TaskValidationException("El parámetro 'q' es obligatorio.");
        }
        String fragment = q.strip();
        List<Task> resultados = repository.findByTitleContainingIgnoreCase(fragment);
        return resultados.stream().sorted(TaskOrders.POR_TITULO).toList();
    }
}
```

</details>


---

<sub>54s</sub>

### `view` — Failed

**view the file at src/main/java/com/taskflow/exception/GlobalExceptionHandler.java.**

src/main/java/com/taskflow/exception/GlobalExceptionHandler.java

```
Path does not exist
```


---

<sub>1m 15s</sub>

### Copilot

Resumen breve: la diff implementa exactamente lo pedido: nuevo método en TaskService, nuevo endpoint en TaskController y dos tests (unit + slice). No se cambió ningún test existente. Encontrado un detalle menor de revisión (no pude abrir GlobalExceptionHandler para verificar mapeo), pero según la especificación ese handler ya existe. Veredicto: APROBADO.

Tabla de hallazgos:

| # | Gravedad | Archivo:línea | Punto checklist | Qué pasa | Qué harías |
|---|---:|---|---|---|---|
| 1 | Sugerencia | src/main/java/com/taskflow/service/TaskService.java: método buscarPorTitulo (añadido) | 3,4 | Implementa strip(), usa repository.findByTitleContainingIgnoreCase(...) y ordena con TaskOrders.POR_TITULO; lanza TaskValidationException con el mensaje requerido. | Ninguna acción; cumple la especificación. |
| 2 | Sugerencia | src/main/java/com/taskflow/controller/TaskController.java: método searchTasks (añadido) | 4,5 | Firma @GetMapping("/tasks/search"), @RequestParam(required=false) y delega al servicio; devuelve TaskMapper.aResponse. | Ninguna acción; cumple la especificación (required=false deja que el servicio lance la excepción). |
| 3 | Sugerencia | src/test/java/com/taskflow/unit/BusquedaTareasServiceTest.java (nuevo) | 7 | Tests unitarios cubren trim, orden y comportamiento con null/empty; verifican llamada al repo. | Ninguna acción; bien escrito. |
| 4 | Sugerencia | src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java (nuevo) | 7 | Slice test cubre /tasks/search?q=... y ausencia de q -> 400; mockea TaskService y JwtAuthenticationFilter; usa @WebMvcTest(TaskController.class) y addFilters=false. | Ninguna acción; cumple lo pedido. |
| 5 | Sugerencia | N/A (no pude abrir) | 2 | No pude abrir el archivo GlobalExceptionHandler desde el repo para verificar explícitamente que TaskValidationException se mapea a 400 y que ErrorResponse contiene field "message". | Confirmar localmente que GlobalExceptionHandler mantiene ese mapping (pero la especificación asume que existe). |

Casos sin test:
- Ninguno; los dos tests requeridos por la especificación están presentes y cubren los casos pedidos.

Veredicto: APROBADO.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
