package com.project.backend.domain.nlp.converter;

import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.dto.response.LlmResDTO;
import com.project.backend.domain.nlp.dto.response.NlpResDTO;
import com.project.backend.domain.nlp.enums.ItemType;
import com.project.backend.domain.nlp.enums.RecurrenceFrequency;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NlpConverter {

    public static NlpResDTO.ParsedItem toParsedItem(LlmResDTO llmResDTO) {
        return NlpResDTO.ParsedItem.builder()
                .type(parseItemType(llmResDTO.type()))
                .title(llmResDTO.title())
                .date(parseDate(llmResDTO.date()))
                .time(parseTime(llmResDTO.time()))
                .hasDeadline(Boolean.TRUE.equals(llmResDTO.hasDeadline()))
                .isRecurring(Boolean.TRUE.equals(llmResDTO.isRecurring()))
                .recurrenceRule(toRecurrenceRule(llmResDTO.recurrenceRule()))
                .isAmbiguous(Boolean.TRUE.equals(llmResDTO.isAmbiguous()))
                .ambiguousReason(llmResDTO.ambiguousReason())
                .options(toAmbiguousOptions(llmResDTO.options()))
                .needsAdditionalInfo(Boolean.TRUE.equals(llmResDTO.needsAdditionalInfo()))
                .additionalInfoType(llmResDTO.additionalInfoType())
                .confidence(llmResDTO.confidence() !=null ? llmResDTO.confidence() : 0.0)
                .build();
    }

    public static NlpResDTO.ParsedItem toParsedItem(LlmResDTO.LlmParsedItem item) {
        return NlpResDTO.ParsedItem.builder()
                .type(parseItemType(item.type()))
                .title(item.title())
                .date(parseDate(item.date()))
                .time(parseTime(item.time()))
                .hasDeadline(Boolean.TRUE.equals(item.hasDeadline()))
                .isRecurring(Boolean.TRUE.equals(item.isRecurring()))
                .recurrenceRule(toRecurrenceRule(item.recurrenceRule()))
                .isAmbiguous(Boolean.TRUE.equals(item.isAmbiguous()))
                .ambiguousReason(item.ambiguousReason())
                .options(toAmbiguousOptions(item.options()))
                .needsAdditionalInfo(Boolean.TRUE.equals(item.needsAdditionalInfo()))
                .additionalInfoType(item.additionalInfoType())
                .confidence(item.confidence() !=null ? item.confidence() : 0.0)
                .build();
    }

    private static ItemType parseItemType(String type) {
        if (type == null) {
            return ItemType.AMBIGUOUS;
        }

        return switch (type.toUpperCase()) {
            case "EVENT" -> ItemType.EVENT;
            case "TODO" -> ItemType.TODO;
            default -> ItemType.AMBIGUOUS;
        };
    }

    private static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(timeStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static NlpReqDTO.RecurrenceRule toRecurrenceRule(LlmResDTO.LlmRecurrenceRule llmRule) {
        if (llmRule == null) {
            return null;
        }

        return NlpReqDTO.RecurrenceRule.builder()
                .frequency(parseFrequency(llmRule.frequency()))
                .daysOfWeek(llmRule.daysOfWeek())
                .interval(llmRule.interval())
                .endDate(parseDate(llmRule.endDate()))
                .build();
    }

    private static RecurrenceFrequency parseFrequency(String frequency) {
        if (frequency == null) {
            return null;
        }

        return switch (frequency.toUpperCase()) {
            case "DAILY" -> RecurrenceFrequency.DAILY;
            case "WEEKLY" -> RecurrenceFrequency.WEEKLY;
            case "MONTHLY" -> RecurrenceFrequency.MONTHLY;
            case "YEARLY" -> RecurrenceFrequency.YEARLY;
            default -> null;
        };
    }

    private static List<NlpResDTO.AmbiguousOption> toAmbiguousOptions(List<LlmResDTO.LlmAmbiguousOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }

        return options.stream()
                .map(opt -> new NlpResDTO.AmbiguousOption(opt.label(), parseItemType(opt.type())))
                .toList();
    }
}
