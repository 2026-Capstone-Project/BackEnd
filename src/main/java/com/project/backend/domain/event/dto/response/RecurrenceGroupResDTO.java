package com.project.backend.domain.event.dto.response;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class RecurrenceGroupResDTO {

    @Builder
    public record DetailRes(
            Long id,
            RecurrenceFrequency frequency,
            Boolean isCustom,
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
    ) {
    }
}
