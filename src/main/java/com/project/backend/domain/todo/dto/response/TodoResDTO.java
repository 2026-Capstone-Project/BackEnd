package com.project.backend.domain.todo.dto.response;

import com.project.backend.domain.todo.enums.Priority;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

public class TodoResDTO {

    @Builder
    public record TodoInfo(
            Long id,
            String title,
            LocalDate dueDate,
            LocalTime dueTime,
            Boolean isAllDay,
            Priority priority,
            String memo,
            Boolean isCompleted,
            Long recurringTodoId
    ) {}
}
