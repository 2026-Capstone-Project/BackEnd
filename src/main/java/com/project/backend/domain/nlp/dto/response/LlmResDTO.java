package com.project.backend.domain.nlp.dto.response;

import java.util.List;

// LLM이 준 원본(JSON)을 그대로 받는 용도
public record LlmResDTO(
        Boolean isMultiple,

        Integer totalCount,

        List<LlmParsedItem> items,

        String type,
        String title,
        String date,
        String time,
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

    public record LlmParsedItem(
            String type,
            String title,
            String date,
            String time,
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
            String frequency,
            List<String> daysOfWeek,
            Integer interval,
            String endDate
    ) {
    }

    public record LlmAmbiguousOption(
            String label,
            String type
    ) {
    }
}

