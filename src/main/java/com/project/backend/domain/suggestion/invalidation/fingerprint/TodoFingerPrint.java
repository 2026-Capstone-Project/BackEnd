package com.project.backend.domain.suggestion.invalidation.fingerprint;

import com.project.backend.domain.todo.entity.Todo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Todo 무효화 판단에 사용하는 비교용 fingerprint
 */
public record TodoFingerPrint(
        LocalDate startDate,
        LocalTime dueTime,
        Boolean isAllDay,
        Long todoRecurrenceGroupId
) implements PlanFingerprint {

    public static TodoFingerPrint from(Todo todo) {
        return new TodoFingerPrint(
                todo.getStartDate(),
                todo.getDueTime(),
                todo.getIsAllDay(),
                todo.getTodoRecurrenceGroup() != null ? todo.getTodoRecurrenceGroup().getId() : null
        );
    }
}