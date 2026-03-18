package com.project.backend.domain.suggestion.vo.fingerprint;

import com.project.backend.domain.common.recurrence.enums.MonthlyWeekdayRule;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.*;
import com.project.backend.global.recurrence.util.RecurrenceUtils;

import java.time.LocalDate;

public record RecurrenceGroupFingerPrint(
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
        Integer occurrenceCount
) {
    public static RecurrenceGroupFingerPrint from(RecurrenceGroup rg) {
        return new RecurrenceGroupFingerPrint(
                rg.getFrequency(),
                rg.getIntervalValue(),
                rg.getDaysOfWeek(),
                rg.getMonthlyType(),
                rg.getDaysOfMonth(),
                rg.getWeekOfMonth(),
                rg.getDayOfWeekInMonth() != null
                        ? RecurrenceUtils.inferWeekdayRule(RecurrenceUtils.parseDaysOfWeek(rg.getDayOfWeekInMonth()))
                        : null,
                rg.getDayOfWeekInMonth(),
                rg.getMonthOfYear(),
                rg.getEndType(),
                rg.getEndDate(),
                rg.getOccurrenceCount()
        );
    }
}
