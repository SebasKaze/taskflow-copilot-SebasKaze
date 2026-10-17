package com.taskflow.unit;

import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusquedaTareasService - tests unitarios")
public class BusquedaTareasServiceTest {

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private com.taskflow.service.TaskService service;

    private static final Long PROYECTO = 1L;

    @Test
    void buscarPorTitulo_trimea_y_ordena() throws TaskValidationException {
        Task t1 = tarea(2L, "Zeta tarea");
        Task t2 = tarea(1L, "Alpha tarea");

        when(repository.findByTitleContainingIgnoreCase("api")).thenReturn(List.of(t1, t2));

        List<Task> resultado = service.buscarPorTitulo("  api ");

        // Debe devolver en orden alfabético por título (Alpha antes que Zeta)
        assertEquals(1L, resultado.get(0).getId());
        assertEquals(2L, resultado.get(1).getId());

        verify(repository).findByTitleContainingIgnoreCase("api");
    }

    @Test
    void buscarPorTitulo_null_o_vacio_lanzaYNoLlamaRepo() {
        assertThrows(TaskValidationException.class, () -> service.buscarPorTitulo(null));
        assertThrows(TaskValidationException.class, () -> service.buscarPorTitulo("   "));
        verify(repository, never()).findByTitleContainingIgnoreCase(anyString());
    }

    private Task tarea(Long id, String title) {
        try {
            return new Task(id, title, "desc", TaskStatus.TODO, Priority.MED, PROYECTO, null, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }
}
