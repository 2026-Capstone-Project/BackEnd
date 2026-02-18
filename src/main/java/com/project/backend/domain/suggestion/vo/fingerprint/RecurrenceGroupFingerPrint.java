package com.project.backend.domain.suggestion.vo.fingerprint;

import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.MonthlyWeekdayRule;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;

import java.time.LocalDate;
import java.util.Set;

public record RecurrenceGroupFingerPrint (
        RecurrenceFrequency frequency,
        Integer intervalValue,
        String daysOfWeek,
        MonthlyType monthlyType,
        String daysOfMonth,
        Integer weekOfMonth,
        MonthlyWeekdayRule monthlyWeekdayRule,
        String dayOfWeekInMonth,
        Integer monthOfYear,
        RecurrenceEndType endType,
        LocalDate endDate,
        Integer occurrenceCount,
        Set<RecurrenceException> exceptionsDate
) {
    public static RecurrenceGroupFingerPrint from(RecurrenceGroup rg) {
        return new RecurrenceGroupFingerPrint(
                rg.getFrequency(),
                rg.getIntervalValue(),
                rg.getDaysOfWeek(),
                rg.getMonthlyType(),
                rg.getDaysOfMonth(),
                rg.getWeekOfMonth(),
                rg.getMonthlyWeekdayRule(),
                rg.getDayOfWeekInMonth(),
                rg.getMonthOfYear(),
                rg.getEndType(),
                rg.getEndDate(),
                rg.getOccurrenceCount(),
                rg.getExceptionDates()
        );
    }
}
