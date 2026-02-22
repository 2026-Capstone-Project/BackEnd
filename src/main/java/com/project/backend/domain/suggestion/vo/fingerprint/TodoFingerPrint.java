package com.project.backend.domain.suggestion.vo.fingerprint;

import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;

import java.time.LocalDate;
import java.time.LocalTime;

public record TodoFingerPrint (
    LocalDate startDate,
    LocalTime dueTime,
    Boolean isAllDay,
    Long todoRecurrenceGroupId
) {
    public static TodoFingerPrint from(Todo todo) {
        return new TodoFingerPrint(
                todo.getStartDate(),
                todo.getDueTime(),
                todo.getIsAllDay(),
                todo.getTodoRecurrenceGroup() != null ? todo.getTodoRecurrenceGroup().getId() : null
        );
    }
}
