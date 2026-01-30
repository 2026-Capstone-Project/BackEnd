package com.project.backend.domain.event.converter;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record RecurrenceGroupSpec(
        RecurrenceFrequency frequency,
        Integer interval,

        List<String> daysOfWeek,

        MonthlyType monthlyType,
        List<Integer> daysOfMonth,
        Integer weekOfMonth,
        List<String> dayOfWeekInMonth,

        Integer monthOfYear,

        RecurrenceEndType endType,
        LocalDate endDate,
        Integer occurrenceCount
) {}
