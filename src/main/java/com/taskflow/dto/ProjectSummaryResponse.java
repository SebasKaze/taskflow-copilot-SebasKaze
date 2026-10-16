package com.taskflow.dto;

import com.taskflow.model.TaskStatus;

import java.util.Map;

/**
 * ProjectSummaryResponse — resumen de un proyecto para el tablero.
 */
public record ProjectSummaryResponse(
        Long projectId,
        String projectName,
        int totalTasks,
        Map<TaskStatus, Integer> byStatus,
        int overdue
) {
}
