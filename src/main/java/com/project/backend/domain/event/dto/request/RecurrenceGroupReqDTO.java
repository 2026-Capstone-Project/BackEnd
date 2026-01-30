package com.project.backend.domain.event.dto.request;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class RecurrenceGroupReqDTO {

    @Builder
    public record CreateReq(
            @NotNull(message = "반복 주기는 필수입니다")
            RecurrenceFrequency frequency,

            Integer intervalValue,

            // WEEKLY: 반복 요일
            List<DayOfWeek> daysOfWeek,

            // MONTHLY: 반복 타입
            MonthlyType monthlyType,
            List<@Min(1) @Max(31) Integer> daysOfMonth,

            @Min(1)
            @Max(5)
            Integer weekOfMonth,
            List<DayOfWeek> dayOfWeekInMonth,

            // YEARLY: 반복 월
            @Min(1)
            @Max(12)
            Integer monthOfYear,

            // 종료 조건
            RecurrenceEndType endType,
            LocalDate endDate,
            @Min(1)
            Integer occurrenceCount
    ) {
        /**
         * 현재 기획에서는 intervalValue 기능(격주, 2일마다 등)을 지원하지 않음.
         * 향후 확장을 위해 필드는 유지하되, 항상 1을 반환.
         */
        public int getIntervalOrDefault() {
            return 1;
        }
    }

    @Builder
    public record UpdateReq(
            RecurrenceFrequency frequency,

            Integer intervalValue,

            // WEEKLY: 반복 요일
            List<DayOfWeek> daysOfWeek,

            // MONTHLY: 반복 타입
            MonthlyType monthlyType,
            List<@Min(1) @Max(31) Integer> daysOfMonth,

            @Min(1)
            @Max(5)
            Integer weekOfMonth,
            List<DayOfWeek> dayOfWeekInMonth,

            // YEARLY: 반복 월
            @Min(1)
            @Max(12)
            Integer monthOfYear,

            // 종료 조건
            RecurrenceEndType endType,
            LocalDate endDate,
            @Min(1)
            Integer occurrenceCount
    ) {
    }
}
