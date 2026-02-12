package com.project.backend.domain.suggestion.vo;

import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.enums.ExceptionType;
import com.project.backend.domain.todo.entity.TodoRecurrenceException;

import java.time.LocalDateTime;

public record RecurrenceSuggestionException (
        Long id,
        ExceptionType exceptionType,
        LocalDateTime startTime
) {
    public static RecurrenceSuggestionException from(RecurrenceException re) {
        return new RecurrenceSuggestionException(
                re.getId(),
                re.getExceptionType(),
                re.getStartTime()
        );
    }

    public static RecurrenceSuggestionException from(TodoRecurrenceException tre) {
        return new RecurrenceSuggestionException(
                tre.getId(),
                tre.getExceptionType(),
                tre.getExceptionDate().atTime(0, 0)
        );
    }
}
