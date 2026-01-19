package com.project.backend.domain.nlp.service;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.exception.NlpErrorCode;
import com.project.backend.domain.nlp.exception.NlpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RecurrenceCalculator {

    private final int defaultMonths = 3;

    private final int maxCount = 365;

    public List<LocalDate> calculate(LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        LocalDate endDate = calculateEndDate(startDate, rule);
        int maxOccurrences = calculateMaxOccurrences(rule);

        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate) && dates.size() < maxOccurrences && dates.size() < maxCount) {
            dates.add(current);
            current = getNextOccurrence(current, startDate, rule);

            if (current == null) {
                break;
            }
        }

        if (dates.size() >= maxCount) {
            log.warn("반복 일정 최대 개수 도달: {}", maxCount);
            throw new NlpException(NlpErrorCode.RECURRENCE_DATE_EXCEEDED);
        }

        log.info("반복 일정 계산 완료 - frequency: {}, count: {}", rule.frequency(), dates.size());
        return dates;
    }

    private LocalDate calculateEndDate(LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        if (rule.endType() == null || rule.endType() == RecurrenceEndType.NEVER) {
            return startDate.plusMonths(defaultMonths);
        }

        if (rule.endType() == RecurrenceEndType.END_BY_DATE && rule.endDate() != null) {
            return rule.endDate();
        }

        if (rule.endType() == RecurrenceEndType.END_BY_COUNT) {
            return startDate.plusYears(10);
        }

        return startDate.plusMonths(defaultMonths);
    }

    private int calculateMaxOccurrences(NlpReqDTO.RecurrenceRule rule) {
        if (rule.endType() == RecurrenceEndType.END_BY_COUNT && rule.occurrenceCount() != null) {
            return rule.occurrenceCount();
        }
        return maxCount;
    }

    private LocalDate getNextOccurrence(LocalDate current, LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        return switch (rule.frequency()) {
            case NONE -> null;
            case DAILY -> getNextDaily(current, rule);
            case WEEKLY -> getNextWeekly(current, startDate, rule);
            case MONTHLY -> getNextMonthly(current, startDate, rule);
            case YEARLY -> getNextYearly(current, rule);
        };
    }

    // ==================== DAILY ====================

    private LocalDate getNextDaily(LocalDate current, NlpReqDTO.RecurrenceRule rule) {
        return current.plusDays(rule.getIntervalOrDefault());
    }

    // ==================== WEEKLY ====================

    private LocalDate getNextWeekly(LocalDate current, LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        List<DayOfWeek> targetDays = parseDaysOfWeek(rule.daysOfWeek());

        if (targetDays.isEmpty()) {
            return current.plusWeeks(rule.getIntervalOrDefault());
        }

        LocalDate next = current.plusDays(1);

        while (true) {
            if (targetDays.contains(next.getDayOfWeek())) {
                LocalDate nextWeekStart = next.with(DayOfWeek.MONDAY);
                LocalDate startWeekStart = startDate.with(DayOfWeek.MONDAY);
                long weeksBetween = (nextWeekStart.toEpochDay() - startWeekStart.toEpochDay()) / 7;

                if (weeksBetween % rule.getIntervalOrDefault() == 0) {
                    return next;
                }
            }
            next = next.plusDays(1);

            if (next.isAfter(current.plusYears(1))) {
                return null;
            }
        }
    }

    private List<DayOfWeek> parseDaysOfWeek(List<String> daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return List.of();
        }

        return daysOfWeek.stream()
                .map(this::toDayOfWeek)
                .filter(d -> d != null)
                .toList();
    }

    private DayOfWeek toDayOfWeek(String day) {
        return switch (day.toUpperCase()) {
            case "MON", "MONDAY", "월" -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY", "화" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY", "수" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY", "목" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY", "금" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY", "토" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY", "일" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    // ==================== MONTHLY ====================

    private LocalDate getNextMonthly(LocalDate current, LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        LocalDate nextMonth = current.plusMonths(rule.getIntervalOrDefault());

        MonthlyType monthlyType = rule.monthlyType() != null
                ? rule.monthlyType()
                : MonthlyType.DAY_OF_MONTH;

        return switch (monthlyType) {
            case DAY_OF_MONTH -> getNextMonthlyByDayOfMonth(nextMonth, rule, startDate);
            case DAY_OF_WEEK -> getNextMonthlyByDayOfWeek(nextMonth, rule, startDate);
        };
    }

    private LocalDate getNextMonthlyByDayOfMonth(LocalDate baseMonth, NlpReqDTO.RecurrenceRule rule, LocalDate startDate) {
        int targetDay = rule.dayOfMonth() != null ? rule.dayOfMonth() : startDate.getDayOfMonth();
        int lastDayOfMonth = baseMonth.lengthOfMonth();
        int actualDay = Math.min(targetDay, lastDayOfMonth);

        return baseMonth.withDayOfMonth(actualDay);
    }

    private LocalDate getNextMonthlyByDayOfWeek(LocalDate baseMonth, NlpReqDTO.RecurrenceRule rule, LocalDate startDate) {
        int weekOfMonth = rule.weekOfMonth() != null ? rule.weekOfMonth() : 1;
        DayOfWeek targetDayOfWeek = rule.dayOfWeekInMonth() != null
                ? toDayOfWeek(rule.dayOfWeekInMonth())
                : startDate.getDayOfWeek();

        if (targetDayOfWeek == null) {
            targetDayOfWeek = DayOfWeek.MONDAY;
        }

        LocalDate firstDayOfMonth = baseMonth.withDayOfMonth(1);

        if (weekOfMonth == -1) {
            return firstDayOfMonth.with(TemporalAdjusters.lastInMonth(targetDayOfWeek));
        } else {
            LocalDate firstTargetDay = firstDayOfMonth.with(TemporalAdjusters.firstInMonth(targetDayOfWeek));
            return firstTargetDay.plusWeeks(weekOfMonth - 1);
        }
    }

    // ==================== YEARLY ====================

    private LocalDate getNextYearly(LocalDate current, NlpReqDTO.RecurrenceRule rule) {
        LocalDate nextYear = current.plusYears(rule.getIntervalOrDefault());

        if (rule.monthOfYear() != null) {
            nextYear = nextYear.withMonth(rule.monthOfYear());
        }

        if (rule.dayOfMonth() != null) {
            int lastDayOfMonth = nextYear.lengthOfMonth();
            int actualDay = Math.min(rule.dayOfMonth(), lastDayOfMonth);
            nextYear = nextYear.withDayOfMonth(actualDay);
        }

        return nextYear;
    }
}
