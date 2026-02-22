package com.project.backend.domain.event.dto.response;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.common.plan.enums.MonthlyWeekdayRule;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class RecurrenceGroupResDTO {

    @Builder
    public record DetailRes(
            Long id,
            RecurrenceFrequency frequency,
            Boolean isCustom,
            Integer interval,

            List<DayOfWeek> daysOfWeek,

            MonthlyType monthlyType,
            List<Integer> daysOfMonth,
            Integer weekOfMonth,
            MonthlyWeekdayRule weekdayRule,
            List<DayOfWeek> dayOfWeekInMonth,

            Integer monthOfYear,

            RecurrenceEndType endType,
            LocalDate endDate,
            Integer occurrenceCount
    ) {
    }
}
