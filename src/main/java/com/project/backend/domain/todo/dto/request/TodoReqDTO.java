package com.project.backend.domain.todo.dto.request;

import com.project.backend.domain.todo.enums.Priority;
import com.project.backend.domain.todo.enums.RecurrenceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class TodoReqDTO {

    public record CreateTodo(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
            String title,

            @NotNull(message = "마감일은 필수입니다.")
            LocalDate dueDate,

            LocalTime dueTime,

            @NotNull(message = "종일 여부는 필수입니다.")
            Boolean isAllDay,

            @NotNull(message = "우선순위는 필수입니다.")
            Priority priority,

            String memo,

            @Valid
            Recurrence recurrence
    ) {}

    public record Recurrence(
            @NotNull(message = "반복 유형은 필수입니다.")
            RecurrenceType type,

            List<String> customDays,

            LocalDate endDate,

            Integer repeatCount
    ) {}
}
