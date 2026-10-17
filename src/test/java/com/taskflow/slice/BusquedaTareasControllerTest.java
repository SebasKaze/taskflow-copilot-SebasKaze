package com.taskflow.slice;

import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.taskflow.controller.TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
public class BusquedaTareasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void get_search_q_presente_devuelve200_y_lista() throws Exception {
        Task t1 = tarea(9L, "Documentar la API con Swagger");
        Task t2 = tarea(5L, "Optimizar consultas de la API");

        when(taskService.buscarPorTitulo("api")).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/tasks/search").param("q", "api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].title").value("Documentar la API con Swagger"))
                .andExpect(jsonPath("$[1].id").value(5))
                .andExpect(jsonPath("$[1].title").value("Optimizar consultas de la API"));
    }

    @Test
    void get_search_q_faltante_devuelve400_con_mensaje() throws Exception {
        when(taskService.buscarPorTitulo(null)).thenThrow(new TaskValidationException("El parámetro 'q' es obligatorio."));

        mockMvc.perform(get("/tasks/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'q' es obligatorio."));
    }

    private Task tarea(Long id, String title) {
        try {
            return new Task(id, title, "desc", TaskStatus.IN_PROGRESS, Priority.MED, 1L, null, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }
}
