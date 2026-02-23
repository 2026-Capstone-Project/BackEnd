package com.project.backend.domain.event.service;

import com.project.backend.domain.event.converter.RecurrenceGroupSpec;
import com.project.backend.domain.event.dto.AdjustedTime;
import com.project.backend.domain.common.plan.enums.MonthlyWeekdayRule;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.event.exception.RecurrenceGroupErrorCode;
import com.project.backend.domain.event.exception.RecurrenceGroupException;
import com.project.backend.global.recurrence.util.RecurrenceUtils;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.util.List;

@Slf4j
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class RecurrenceTimeAdjuster {

    public static AdjustedTime adjust(
            LocalDateTime start,
            LocalDateTime end,
            RecurrenceGroupSpec spec
    ) {
        if (spec == null || spec.frequency() == null || spec.frequency() == RecurrenceFrequency.DAILY) {
            return new AdjustedTime(start, end);
        }

        Duration duration = Duration.between(start, end);

        LocalDateTime adjustedStart = switch (spec.frequency()) {
            case WEEKLY -> adjustWeekly(start, spec);
            case MONTHLY -> adjustMonthly(start, spec);
            case YEARLY -> adjustYearly(start, spec);
            default -> start;
        };

        return new AdjustedTime(
                adjustedStart,
                adjustedStart.plus(duration)
        );
    }

    /**
     * WEEKLY 반복 보정
     * 규칙:
     * 1. startTime이 속한 "주"를 기준으로 한다.
     * 2. 그 주 안에 반복 요일이 있으면 → 그중 가장 빠른 날짜로 보정
     *    (과거 허용, 단 이전 주는 절대 안 감)
     * 3. 그 주에 없으면 → 다음 주의 가장 빠른 반복 요일
     */
    private static LocalDateTime adjustWeekly(
            LocalDateTime base,
            RecurrenceGroupSpec spec
    ) {
        List<DayOfWeek> targets = spec.daysOfWeek();
        if (targets == null || targets.isEmpty()) {
            return base;
        }

        // 주 시작/끝 (ISO 기준: 월~일)
        LocalDate weekStart = base.toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        // 같은 주 안에서 반복 요일 찾기 (과거 허용)
        LocalDate candidateInSameWeek = targets.stream()
                .map(weekStart::with)
                .filter(date -> !date.isBefore(weekStart) && !date.isAfter(weekEnd))
                .min(LocalDate::compareTo)
                .orElse(null);

        if (candidateInSameWeek != null) {
            return LocalDateTime.of(candidateInSameWeek, base.toLocalTime());
        }

        // 같은 주에 없으면 → 다음 주
        LocalDate nextWeekStart = weekStart.plusWeeks(1);

        LocalDate nextWeekCandidate = targets.stream()
                .map(nextWeekStart::with)
                .min(LocalDate::compareTo)
                .orElseThrow();

        return LocalDateTime.of(nextWeekCandidate, base.toLocalTime());
    }


    private static LocalDateTime adjustMonthly(
            LocalDateTime base,
            RecurrenceGroupSpec spec
    ) {
        return switch (spec.monthlyType()) {
            case DAY_OF_MONTH -> adjustMonthlyByDayOfMonth(base, spec);
            case DAY_OF_WEEK -> adjustMonthlyByNthWeekday(base, spec);
        };
    }


    /**
     * MONTHLY - DAY_OF_MONTH
     * 규칙:
     * 1. startTime이 속한 "월" 안에서 먼저 찾는다.
     * 2. 그 월에 해당 일이 있으면 → 가장 빠른 날짜 선택 (과거 허용)
     * 3. 없으면 → 다음 달
     */
    private static LocalDateTime adjustMonthlyByDayOfMonth(
            LocalDateTime base,
            RecurrenceGroupSpec spec
    ) {
        List<Integer> days = spec.daysOfMonth();
        if (days == null || days.isEmpty()) {
            return base;
        }

        YearMonth currentMonth = YearMonth.from(base);

        // 같은 달에서 찾기
        LocalDate candidate = days.stream()
                .filter(day -> day <= currentMonth.lengthOfMonth())
                .map(currentMonth::atDay)
                .min(LocalDate::compareTo)
                .orElse(null);

        if (candidate != null) {
            return LocalDateTime.of(candidate, base.toLocalTime());
        }

        // 없으면 다음 달
        YearMonth nextMonth = currentMonth.plusMonths(1);
        int safeDay = Math.min(
                days.stream().min(Integer::compareTo).orElseThrow(),
                nextMonth.lengthOfMonth()
        );

        return LocalDateTime.of(
                nextMonth.atDay(safeDay),
                base.toLocalTime()
        );
    }

    /**
     * MONTHLY - DAY_OF_WEEK (예: 매월 둘째 수요일)
     * 규칙:
     * 1. startTime이 속한 "월" 안에서 먼저 찾는다.
     * 2. 있으면 → 그 날짜 사용 (과거 허용)
     * 3. 없으면 → 다음 달
     */
    private static LocalDateTime adjustMonthlyByNthWeekday(
            LocalDateTime base,
            RecurrenceGroupSpec spec
    ) {
        int week = spec.weekOfMonth();
        MonthlyWeekdayRule rule = RecurrenceUtils.inferWeekdayRule(spec.dayOfWeekInMonth());

        if (rule == null) {
            return base;
        }

        // rule -> 실제 요일 범위
        List<DayOfWeek> targetDays = switch (rule) {
            case SINGLE -> spec.dayOfWeekInMonth();
            case WEEKDAY -> List.of(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
            );
            case WEEKEND -> List.of(
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY
            );
            case ALL_DAYS -> List.of(DayOfWeek.values());
        };

        if (targetDays == null || targetDays.isEmpty()) {
            return base;
        }

        YearMonth month = YearMonth.from(base);

        // 해당 월의 n번째 주 시작 (월요일 기준)
        LocalDate startOfNthWeek = getNthWeekday(month, week, DayOfWeek.MONDAY);
        if (startOfNthWeek == null) {
            throw new IllegalStateException("Invalid weekOfMonth");
        }

        LocalDate endOfNthWeek = startOfNthWeek.plusDays(6);

        // 그 주 안에서 rule에 맞는 첫 번째 요일 선택
        for (LocalDate d = startOfNthWeek;
             !d.isAfter(endOfNthWeek);
             d = d.plusDays(1)) {

            if (targetDays.contains(d.getDayOfWeek())) {
                return LocalDateTime.of(d, base.toLocalTime());
            }
        }

        throw new RecurrenceGroupException(RecurrenceGroupErrorCode.FAIL_ADJUSTMENT_DAY_OF_WEEK);
    }

    private static LocalDate getNthWeekday(
            YearMonth ym,
            int week,
            DayOfWeek dayOfWeek
    ) {
        LocalDate firstDay = ym.atDay(1);

        int diff =
                dayOfWeek.getValue() - firstDay.getDayOfWeek().getValue();
        if (diff < 0) diff += 7;

        LocalDate first = firstDay.plusDays(diff);
        LocalDate result = first.plusWeeks(week - 1);

        return result.getMonth() == ym.getMonth() ? result : null;
    }


    /**
     * YEARLY 반복 보정
     * 규칙:
     * 1. startTime이 속한 "연도" 안에서 먼저 찾는다.
     * 2. 있으면 → 그 날짜 사용 (과거 허용)
     * 3. 없으면 → 다음 해
     */
    private static LocalDateTime adjustYearly(
            LocalDateTime base,
            RecurrenceGroupSpec spec
    ) {
        Integer month = spec.monthOfYear();
        List<Integer> days = spec.daysOfMonth();

        if (month == null || days == null || days.isEmpty()) {
            return base;
        }

        for (int year = base.getYear(); year <= base.getYear() + 1; year++) {
            YearMonth ym = YearMonth.of(year, month);

            for (Integer day : days) {
                int safeDay = Math.min(day, ym.lengthOfMonth());
                LocalDateTime candidate = LocalDateTime.of(
                        year, month, safeDay,
                        base.getHour(), base.getMinute(), base.getSecond(), base.getNano()
                );

                if (year == base.getYear() || !candidate.isBefore(base)) {
                    return candidate;
                }
            }
        }

        throw new RecurrenceGroupException(RecurrenceGroupErrorCode.FAIL_ADJUSTMENT_YEARLY);
    }
}
