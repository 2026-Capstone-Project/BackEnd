package com.project.backend.domain.event.dto;

import com.project.backend.domain.common.recurrence.enums.MonthlyType;

import java.time.DayOfWeek;
import java.util.List;

public record FrequencyFields (
        List<DayOfWeek> daysOfWeek,
        MonthlyType monthlyType,
        List<Integer> daysOfMonth,
        Integer weekOfMonth,
        List<DayOfWeek> dayOfWeekInMonth,
        Integer monthOfYear
){
    public static FrequencyFields empty() {
        return new FrequencyFields(null, null, null, null, null, null);
    }

    public static FrequencyFields weekly(List<DayOfWeek> daysOfWeek) {
        return new FrequencyFields(daysOfWeek, null, null, null, null, null);
    }

    public static FrequencyFields monthlyByDayOfMonth(List<Integer> daysOfMonth) {
        return new FrequencyFields(null, MonthlyType.DAY_OF_MONTH, daysOfMonth, null, null, null);
    }

    public static FrequencyFields monthlyByDayOfWeek(
            Integer weekOfMonth,
            List<DayOfWeek> dayOfWeekInMonth
    ) {
        return new FrequencyFields(null, MonthlyType.DAY_OF_WEEK, null, weekOfMonth, dayOfWeekInMonth, null);
    }

    public static FrequencyFields yearly(Integer monthOfYear) {
        return new FrequencyFields(null, null, null, null, null, monthOfYear);
    }
}
