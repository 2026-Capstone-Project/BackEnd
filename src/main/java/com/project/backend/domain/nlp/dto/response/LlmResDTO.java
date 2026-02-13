package com.project.backend.domain.nlp.dto.response;

import lombok.Builder;

import java.util.List;

// LLM이 준 원본(JSON)을 그대로 받는 용도
public record LlmResDTO(
        Boolean isMultiple,

        Integer totalCount,

        List<LlmParsedItem> items,

        String type,
        String title,
        String date,
        String startTime,
        String endTime,
        Integer durationMinutes,
        Boolean isAllDay,
        Boolean hasDeadline,

        Boolean isRecurring,
        LlmRecurrenceRule recurrenceRule,

        Boolean isAmbiguous,
        String ambiguousReason,
        List<LlmAmbiguousOption> options,

        Boolean needsAdditionalInfo,
        String additionalInfoType,
        Double confidence
) {
    public boolean isSingleItem() {
        return (isMultiple == null || !isMultiple) && (items == null || items.isEmpty());
    }

    @Builder
    public record LlmParsedItem(
            String type,
            String title,
            String date,
            String startTime,
            String endTime,
            Integer durationMinutes,
            Boolean isAllDay,
            Boolean hasDeadline,

            Boolean isRecurring,
            LlmRecurrenceRule recurrenceRule,

            Boolean isAmbiguous,
            String ambiguousReason,
            List<LlmAmbiguousOption> options,

            Boolean needsAdditionalInfo,
            String additionalInfoType,
            Double confidence
    ) {
    }

    public record LlmRecurrenceRule(
            // 반복 주기: DAILY, WEEKLY, MONTHLY, YEARLY
            String frequency,

            // 반복 간격 (기본값: 1)
            Integer intervalValue,

            // WEEKLY: 반복 요일 ["MONDAY", "WEDNESDAY", "FRIDAY"]
            List<String> daysOfWeek,

            // MONTHLY: 반복 타입 (DAY_OF_MONTH, DAY_OF_WEEK)
            String monthlyType,

            // MONTHLY (DAY_OF_MONTH): 반복 일자 [1, 15, 30]
            List<Integer> daysOfMonth,

            // MONTHLY (DAY_OF_WEEK): N번째 주 (1~5, -1은 마지막)
            Integer weekOfMonth,

            // MONTHLY (DAY_OF_WEEK): 반복 요일 "MONDAY"
            String dayOfWeekInMonth,

            // YEARLY: 반복 월 (1~12)
            Integer monthOfYear,

            // 종료 조건: NEVER, END_BY_DATE, END_BY_COUNT
            String endType,

            // END_BY_DATE: 종료 날짜
            String endDate,

            // END_BY_COUNT: 반복 횟수
            Integer occurrenceCount
    ) {
    }

    public record LlmAmbiguousOption(
            String label,
            String type
    ) {
    }
}

