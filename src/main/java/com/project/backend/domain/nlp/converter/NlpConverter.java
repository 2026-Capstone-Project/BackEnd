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
import java.util.Map;
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
        LocalTime startTime = parseTime(llmResDTO.startTime());
        LocalTime endTime = resolveEndTime(startTime, parseTime(llmResDTO.endTime()));

        return NlpResDTO.ParsedItem.builder()
                .itemId(itemId)
                .type(parseItemType(llmResDTO.type()))
                .title(llmResDTO.title())
                .date(parseDate(llmResDTO.date()))
                .startTime(startTime)
                .endTime(endTime)
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
        LocalTime startTime = parseTime(item.startTime());
        LocalTime endTime = resolveEndTime(startTime, parseTime(item.endTime()));

        return NlpResDTO.ParsedItem.builder()
                .itemId(itemId)
                .type(parseItemType(item.type()))
                .title(item.title())
                .date(parseDate(item.date()))
                .startTime(startTime)
                .endTime(endTime)
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

    private static LocalTime resolveEndTime(LocalTime startTime, LocalTime endTime) {
        if (endTime != null) {
            return endTime;
        }
        if (startTime != null) {
            return startTime.plusHours(1);
        }
        return null;
    }

    private static NlpReqDTO.RecurrenceRule toRecurrenceRule(LlmResDTO.LlmRecurrenceRule llmRule) {
        if (llmRule == null) {
            return null;
        }

        return NlpReqDTO.RecurrenceRule.builder()
                .frequency(parseFrequency(llmRule.frequency()))
                .intervalValue(llmRule.intervalValue())
                .daysOfWeek(convertDaysOfWeek(llmRule.daysOfWeek()))
                .monthlyType(parseMonthlyType(llmRule.monthlyType()))
                .daysOfMonth(llmRule.daysOfMonth())
                .weekOfMonth(llmRule.weekOfMonth())
                .dayOfWeekInMonth(convertDayOfWeek(llmRule.dayOfWeekInMonth()))
                .monthOfYear(llmRule.monthOfYear())
                .endType(parseEndType(llmRule.endType(), llmRule.endDate()))
                .endDate(parseDate(llmRule.endDate()))
                .occurrenceCount(llmRule.occurrenceCount())
                .build();
    }

    private static MonthlyType parseMonthlyType(String monthlyType) {
        if (monthlyType == null || monthlyType.isBlank()) {
            return null;
        }
        try {
            return MonthlyType.valueOf(monthlyType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static RecurrenceEndType parseEndType(String endType, String endDate) {
        if (endType != null && !endType.isBlank()) {
            try {
                return RecurrenceEndType.valueOf(endType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 파싱 실패 시 아래 로직으로 폴백
            }
        }
        // endType이 없으면 endDate 유무로 추론 (하위 호환)
        return endDate != null ? RecurrenceEndType.END_BY_DATE : RecurrenceEndType.NEVER;
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

    // 3글자 → 전체 이름 매핑
    private static final Map<String, String> DAY_SHORT_TO_FULL = Map.of(
            "MON", "MONDAY",
            "TUE", "TUESDAY",
            "WED", "WEDNESDAY",
            "THU", "THURSDAY",
            "FRI", "FRIDAY",
            "SAT", "SATURDAY",
            "SUN", "SUNDAY"
    );

    /**
     * 요일 문자열 리스트를 전체 이름(MONDAY 등)으로 정규화.
     * 3글자(MON)와 전체(MONDAY) 둘 다 지원.
     */
    private static List<String> convertDaysOfWeek(List<String> daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return null;
        }

        return daysOfWeek.stream()
                .map(NlpConverter::convertDayOfWeek)
                .filter(day -> day != null)
                .toList();
    }

    /**
     * 단일 요일 문자열을 전체 이름(MONDAY 등)으로 정규화.
     * 3글자(MON)와 전체(MONDAY) 둘 다 지원.
     */
    private static String convertDayOfWeek(String day) {
        if (day == null || day.isBlank()) {
            return null;
        }
        String upper = day.toUpperCase();
        return DAY_SHORT_TO_FULL.getOrDefault(upper, upper);
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
