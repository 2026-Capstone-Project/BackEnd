package com.project.backend.domain.suggestion.util;

import com.project.backend.domain.suggestion.enums.RecurrencePatternType;
import com.project.backend.domain.suggestion.vo.SuggestionPattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SuggestionAnchorUtil {

    public static LocalDate computeAnchorDate(
            LocalDateTime lastStart,
            RecurrencePatternType patternType,
            SuggestionPattern primary
    ) {
        LocalDate lastDate = lastStart.toLocalDate();

        return switch (patternType) {
            case N_INTERVAL -> {
                Integer dayDiff = primary.dayDiff();
                if (dayDiff == null) yield null;
                // 다음 발생일
                yield lastDate.plusDays(dayDiff);
            }
            case WEEKLY_SET -> {
                Integer weekDiff = primary.weekDiff();
                Set<DayOfWeek> dayOfWeekSet = primary.dayOfWeekSet();
                if (weekDiff == null || dayOfWeekSet == null || dayOfWeekSet.isEmpty()) yield null;

                DayOfWeek firstDayOfWeek = dayOfWeekSet.stream()
                        .min(Comparator.comparingInt(DayOfWeek::getValue))
                        .orElseThrow();

                long baseEpochWeek = WeekEpochUtil.toEpochWeek(lastDate);
                long targetEpochWeek = baseEpochWeek + weekDiff;

                // "다음 패키지의 첫날" (월·화면 월요일)
                yield dateOf(targetEpochWeek, firstDayOfWeek);
            }
            case MONTHLY_DAY -> {
                Integer monthDiff = primary.monthDiff();
                Set<Integer> dayOfMonthSet = primary.dayOfMonthSet();
                if (monthDiff == null || dayOfMonthSet == null || dayOfMonthSet.isEmpty()) yield null;

                int firstDayOfMonth = dayOfMonthSet.stream()
                        .min(Integer::compareTo)
                        .orElseThrow();
                YearMonth targetYm = YearMonth.from(lastDate).plusMonths(monthDiff);

                // TODO : 29/30/31 일 때, 일단 만약 대상 달에 그 날짜가 없다면 Null 반환
                yield resolveMonthlyDayByPolicy(targetYm, firstDayOfMonth);
            }
        };
    }

    public static Integer computeLeadDays(
            RecurrencePatternType patternType,
            SuggestionPattern primary
    ) {
        if (patternType == RecurrencePatternType.N_INTERVAL) {
            Integer dayDiff = primary.dayDiff();
            if (dayDiff == null) return null;
            return Math.min(dayDiff, 7);
        }
        return 7;
    }

    private static LocalDate fromEpochWeekStart(long epochWeek) {
        LocalDate epochStart = LocalDate.of(1970, 1, 5); // 월요일
        return epochStart.plusWeeks(epochWeek);
    }

    private static LocalDate dateOf(long epochWeek, DayOfWeek dayOfWeek) {
        LocalDate weekStart = fromEpochWeekStart(epochWeek); // 월요일
        return weekStart.plusDays(dayOfWeek.getValue() - 1L);
    }

    // "월별 날짜 누락" 공용 정책(예: clamp)
    private static LocalDate resolveMonthlyDayByPolicy(YearMonth ym, int dayOfMonth) {
        if (dayOfMonth > ym.lengthOfMonth()) {
            return null;
        }
        return ym.atDay(dayOfMonth);
    }
}
