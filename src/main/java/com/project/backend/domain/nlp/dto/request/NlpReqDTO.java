package com.project.backend.domain.nlp.dto.request;

import com.project.backend.domain.event.enums.EventColor;
import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.nlp.enums.ItemType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class NlpReqDTO {

    public record ParseReq(
            @NotBlank(message = "입력 텍스트는 필수입니다.")
            String text,

            LocalDate baseDate
    ) {}

    public record ConfirmReq(
            @Valid
            @NotEmpty(message = "저장할 항목이 없습니다.")
            List<ConfirmItem> items
    ) {}

    public record ConfirmItem(
            @NotNull(message = "타입은 필수입니다.")
            ItemType type,

            @NotBlank(message = "제목은 필수입니다.")
            String title,

            @NotNull(message = "날짜는 필수입니다.")
            LocalDate date,

            LocalTime time,

            LocalTime startTime,

            LocalTime endTime,

            Integer durationMinutes,

            boolean isAllDay,

            boolean isRecurring,

            RecurrenceRule recurrenceRule,

            EventColor color
    ) {
        public LocalTime getStartTimeOrDefault() {
            return startTime != null ? startTime : time;
        }

        public EventColor getColorOrDefault() {
            return color != null ? color : EventColor.BLUE;
        }
    }

    @Builder
    public record RecurrenceRule(
            @NotNull(message = "반복 주기는 필수입니다")
            RecurrenceFrequency frequency,

            // 반복 간격 (기본값: 1, 향후 격주/2일마다 등 지원 예정)
            Integer intervalValue,

            // WEEKLY: 반복 요일 ["MONDAY", "WEDNESDAY", "FRIDAY"]
            List<String> daysOfWeek,

            // MONTHLY: 반복 타입
            MonthlyType monthlyType,

            // MONTHLY (DAY_OF_MONTH): 반복 일자 [1, 15, 30]
            List<Integer> daysOfMonth,

            // MONTHLY (DAY_OF_WEEK): N번째 주 (1~5, -1은 마지막)
            Integer weekOfMonth,

            // MONTHLY (DAY_OF_WEEK): 반복 요일 "MONDAY"
            String dayOfWeekInMonth,

            // YEARLY: 반복 월 (1~12)
            Integer monthOfYear,

            // 종료 조건
            RecurrenceEndType endType,

            // END_BY_DATE: 종료 날짜
            LocalDate endDate,

            // END_BY_COUNT: 반복 횟수
            Integer occurrenceCount
    ) {
        /**
         * 반복 간격 기본값 반환.
         * 현재 기획에서는 intervalValue 기능을 지원하지 않아 항상 1을 반환.
         */
        public int getIntervalOrDefault() {
            return intervalValue != null ? intervalValue : 1;
        }

        /**
         * 종료 조건 기본값 반환.
         * 명시되지 않으면 NEVER (무한 반복, 기본 3개월)
         */
        public RecurrenceEndType getEndTypeOrDefault() {
            return endType != null ? endType : RecurrenceEndType.NEVER;
        }
    }
}
