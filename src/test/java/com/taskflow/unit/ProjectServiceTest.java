package com.taskflow.unit;

import com.taskflow.dto.ProjectSummaryResponse;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    ProjectRepository projectRepository;
    @Mock
    TaskRepository taskRepository;
    @Mock
    UserRepository userRepository;
    @InjectMocks
    ProjectService projectService;

    @Test
    void resumen_conTareas_yVencida() throws Exception {
        Project p = new Project(2L, "App Móvil", "d", 2L, null);
        Task t1 = new Task(1L, "T-1", "d", TaskStatus.TODO, Priority.MED, 2L, 1L, null);
        Task t2 = new Task(2L, "T-2", "d", TaskStatus.IN_PROGRESS, Priority.MED, 2L, 1L, LocalDate.now().minusDays(1));
        Task t3 = new Task(3L, "T-3", "d", TaskStatus.IN_PROGRESS, Priority.MED, 2L, 1L, null);
        Task t4 = new Task(4L, "T-4", "d", TaskStatus.DONE, Priority.MED, 2L, 1L, LocalDate.now().minusDays(10));
        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(t1,t2,t3,t4));

        ProjectSummaryResponse res = projectService.resumen(p);
        assertEquals(4, res.totalTasks());
        assertEquals(1, res.byStatus().get(TaskStatus.TODO).intValue());
        assertEquals(2, res.byStatus().get(TaskStatus.IN_PROGRESS).intValue());
        assertEquals(1, res.byStatus().get(TaskStatus.DONE).intValue());
        assertEquals(1, res.overdue());
    }

    @Test
    void resumen_sinTareas_devuelveCeros() {
        Project p = new Project(3L, "P3", "d", 1L, null);
        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());

        ProjectSummaryResponse res = projectService.resumen(p);
        assertEquals(0, res.totalTasks());
        for (TaskStatus s : TaskStatus.values()) {
            assertEquals(0, res.byStatus().get(s).intValue());
        }
        assertEquals(0, res.overdue());
    }
}
