package com.project.backend.domain.nlp.converter;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.dto.response.LlmResDTO;
import com.project.backend.domain.nlp.dto.response.NlpResDTO;
import com.project.backend.domain.nlp.enums.ItemType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NlpConverter {

    public static NlpResDTO.ParsedItem toParsedItem(LlmResDTO llmResDTO) {
        return toParsedItem(llmResDTO, UUID.randomUUID().toString());
    }

    public static NlpResDTO.ParsedItem toParsedItem(LlmResDTO.LlmParsedItem item) {
        return toParsedItem(item, UUID.randomUUID().toString());
    }

    public static NlpResDTO.ParsedItem toParsedItem(LlmResDTO llmResDTO, String itemId) {
        return NlpResDTO.ParsedItem.builder()
                .itemId(itemId)
                .type(parseItemType(llmResDTO.type()))
                .title(llmResDTO.title())
                .date(parseDate(llmResDTO.date()))
                .startTime(parseTime(llmResDTO.startTime()))
                .endTime(parseTime(llmResDTO.endTime()))
                .durationMinutes(llmResDTO.durationMinutes())
                .isAllDay(Boolean.TRUE.equals(llmResDTO.isAllDay()))
                .hasDeadline(Boolean.TRUE.equals(llmResDTO.hasDeadline()))
                .isRecurring(Boolean.TRUE.equals(llmResDTO.isRecurring()))
                .recurrenceRule(toRecurrenceRule(llmResDTO.recurrenceRule()))
                .isAmbiguous(Boolean.TRUE.equals(llmResDTO.isAmbiguous()))
                .ambiguousReason(llmResDTO.ambiguousReason())
                .options(toAmbiguousOptions(llmResDTO.options()))
                .needsAdditionalInfo(Boolean.TRUE.equals(llmResDTO.needsAdditionalInfo()))
                .additionalInfoType(llmResDTO.additionalInfoType())
                .confidence(llmResDTO.confidence() != null ? llmResDTO.confidence() : 0.0)
                .build();
    }

    public static NlpResDTO.ParsedItem toParsedItem(LlmResDTO.LlmParsedItem item, String itemId) {
        return NlpResDTO.ParsedItem.builder()
                .itemId(itemId)
                .type(parseItemType(item.type()))
                .title(item.title())
                .date(parseDate(item.date()))
                .startTime(parseTime(item.startTime()))
                .endTime(parseTime(item.endTime()))
                .durationMinutes(item.durationMinutes())
                .isAllDay(Boolean.TRUE.equals(item.isAllDay()))
                .hasDeadline(Boolean.TRUE.equals(item.hasDeadline()))
                .isRecurring(Boolean.TRUE.equals(item.isRecurring()))
                .recurrenceRule(toRecurrenceRule(item.recurrenceRule()))
                .isAmbiguous(Boolean.TRUE.equals(item.isAmbiguous()))
                .ambiguousReason(item.ambiguousReason())
                .options(toAmbiguousOptions(item.options()))
                .needsAdditionalInfo(Boolean.TRUE.equals(item.needsAdditionalInfo()))
                .additionalInfoType(item.additionalInfoType())
                .confidence(item.confidence() != null ? item.confidence() : 0.0)
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
                .interval(llmRule.interval())
                .daysOfWeek(llmRule.daysOfWeek())
                .monthlyType(MonthlyType.DAY_OF_MONTH)
                .dayOfMonth(null)
                .weekOfMonth(null)
                .dayOfWeekInMonth(null)
                .monthOfYear(null)
                .endType(llmRule.endDate() != null ? RecurrenceEndType.END_BY_DATE : RecurrenceEndType.NEVER)
                .endDate(parseDate(llmRule.endDate()))
                .occurrenceCount(null)
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
