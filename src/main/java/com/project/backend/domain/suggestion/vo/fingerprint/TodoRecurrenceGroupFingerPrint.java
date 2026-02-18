package com.project.backend.domain.suggestion.vo.fingerprint;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.todo.entity.TodoRecurrenceException;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;

import java.time.LocalDate;
import java.util.Set;

public record TodoRecurrenceGroupFingerPrint(
        RecurrenceFrequency frequency,
        Integer intervalValue,
        String daysOfWeek,
        MonthlyType monthlyType,
        String daysOfMonth,
        Integer weekOfMonth,
        String dayOfWeekInMonth,
        RecurrenceEndType endType,
        LocalDate endDate,
        Integer occurrenceCount,
        Set<TodoRecurrenceException> exceptionsDates
) {
    public static TodoRecurrenceGroupFingerPrint from(TodoRecurrenceGroup trg) {
        return new TodoRecurrenceGroupFingerPrint(
                trg.getFrequency(),
                trg.getIntervalValue(),
                trg.getDaysOfWeek(),
                trg.getMonthlyType(),
                trg.getDaysOfMonth(),
                trg.getWeekOfMonth(),
                trg.getDayOfWeekInMonth(),
                trg.getEndType(),
                trg.getEndDate(),
                trg.getOccurrenceCount(),
                trg.getExceptionDates()
        );
    }
}
