package com.project.backend.domain.nlp.dto.response;

import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.enums.ItemType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class NlpResDTO {

    @Builder
    public record ParseRes(
            boolean isMultiple,
            int totalCount,
            List<ParsedItem> items
    ) {
        public static ParseRes single(ParsedItem item) {
            return ParseRes.builder()
                    .isMultiple(false)
                    .totalCount(1)
                    .items(List.of(item))
                    .build();
        }
        public static ParseRes multiple(List<ParsedItem> items) {
            return ParseRes.builder()
                    .isMultiple(true)
                    .totalCount(items.size())
                    .items(items)
                    .build();
        }
    }

    public record ParsedItem(
            ItemType type,
            String title,
            LocalDate date,
            LocalTime time,
            boolean hasDeadline,

            boolean isRecurring,
            NlpReqDTO.RecurrenceRule recurrenceRule,

            boolean isAmbiguous,
            String ambiguousReason,
            List<AmbiguousOption> options,

            boolean needsAdditionalInfo,
            String additionalInfoType,

            double confidence
    ) {}

    public record AmbiguousOption(
            String label,
            ItemType type
    ) {}

    public record ConfirmRes(
            int totalCount,
            int successCount,
            int failCount,
            List<ConfirmResult> results,
            String message
    ) {}

    @Builder
    public record ConfirmResult(
            List<Long> ids,
            ItemType type,
            String title,
            int count,
            boolean success,
            String errorMessage
    ) {
        public static ConfirmResult success(List<Long> ids, ItemType type, String title) {
            return ConfirmResult.builder()
                    .ids(ids)
                    .type(type)
                    .title(title)
                    .count(ids.size())
                    .success(true)
                    .errorMessage(null)
                    .build();
        }
        public static ConfirmResult failure(ItemType type, String title, String errorMessage) {
            return ConfirmResult.builder()
                    .ids(null)
                    .type(type)
                    .title(title)
                    .count(0)
                    .success(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }


}
