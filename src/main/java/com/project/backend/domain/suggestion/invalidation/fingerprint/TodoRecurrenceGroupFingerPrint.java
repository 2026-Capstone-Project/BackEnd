package com.project.backend.domain.suggestion.invalidation.fingerprint;

import com.project.backend.domain.common.recurrence.enums.MonthlyType;
import com.project.backend.domain.common.recurrence.enums.RecurrenceEndType;
import com.project.backend.domain.common.recurrence.enums.RecurrenceFrequency;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;

import java.time.LocalDate;

/**
 * 반복 할 일 무효화 판단에 사용하는 비교용 fingerprint
 */
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
        Integer occurrenceCount
) implements GroupFingerprint {

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
                trg.getOccurrenceCount()
        );
    }
}