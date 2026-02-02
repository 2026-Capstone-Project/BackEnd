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

            // intervalValue 필드는 향후 확장을 위해 유지하되, 현재는 무시됨 (항상 1 사용)
            Integer interval,

            // WEEKLY: 반복 요일
            List<String> daysOfWeek,

            // MONTHLY: 반복 타입
            MonthlyType monthlyType,
            List<Integer> daysOfMonth,
            Integer weekOfMonth,
            String dayOfWeekInMonth,

            // YEARLY: 반복 월
            Integer monthOfYear,

            // 종료 조건
            RecurrenceEndType endType,
            LocalDate endDate,
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
}
