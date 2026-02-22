package com.project.backend.domain.suggestion.util;

import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.suggestion.enums.RecurrencePatternType;
import com.project.backend.domain.suggestion.vo.RecurrenceSuggestionCandidate;
import com.project.backend.domain.suggestion.vo.SuggestionPattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SuggestionAnchorUtil {

    /**
     * - N_INTERVAL: 다음 발생일 1개 (size=1)
     * - WEEKLY_SET: dayOfWeekSet 개수만큼 (size=|set|)
     * - MONTHLY_DAY: dayOfMonthSet 개수만큼 (size=|set|)  ※ 존재하지 않는 날짜면 null 원소로 들어갈 수 있음
     */
    public static List<LocalDate> computeAnchorDate(
            LocalDateTime lastStart,
            RecurrencePatternType patternType,
            SuggestionPattern primary
    ) {
        LocalDate lastDate = lastStart.toLocalDate();

        return switch (patternType) {

            case N_INTERVAL -> {
                Integer dayDiff = primary.dayDiff();
                if (dayDiff == null) yield null;
                yield List.of(lastDate.plusDays(dayDiff));
            }

            case WEEKLY_SET -> {
                Integer weekDiff = primary.weekDiff();
                Set<DayOfWeek> dayOfWeekSet = primary.dayOfWeekSet();
                if (weekDiff == null || dayOfWeekSet == null || dayOfWeekSet.isEmpty()) yield null;

                long baseEpochWeek = WeekEpochUtil.toEpochWeek(lastDate);
                long targetEpochWeek = baseEpochWeek + weekDiff;

                // "다음 패키지"에서 요일 set만큼 날짜를 모두 반환 (월·화면 월요일/화요일 둘 다)
                yield dayOfWeekSet.stream()
                        .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                        .map(dow -> dateOf(targetEpochWeek, dow))
                        .toList();
            }

            case MONTHLY_DAY -> {
                Integer monthDiff = primary.monthDiff();
                Set<Integer> dayOfMonthSet = primary.dayOfMonthSet();
                if (monthDiff == null || dayOfMonthSet == null || dayOfMonthSet.isEmpty()) yield null;

                YearMonth targetYm = YearMonth.from(lastDate).plusMonths(monthDiff);
                int lastDay = targetYm.lengthOfMonth();

                // 하나라도 존재하지 않으면 그냥 null 반환
                if (dayOfMonthSet.stream().anyMatch(day -> day == null || day < 1 || day > lastDay)) {
                    yield null;
                }

                yield dayOfMonthSet.stream()
                        .sorted()
                        .map(targetYm::atDay)
                        .toList();
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

    public static Integer computeLeadDays(
            RecurrenceSuggestionCandidate candidate
    ) {
        if (candidate.getFrequency() == RecurrenceFrequency.DAILY) {
            return Math.min(candidate.getIntervalValue(), 7);
        } else {
            return 7;
        }
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
    // 지금은 "없으면 null" 정책 유지
    private static LocalDate resolveMonthlyDayByPolicy(YearMonth ym, int dayOfMonth) {
        if (dayOfMonth > ym.lengthOfMonth()) {
            return null;
        }
        return ym.atDay(dayOfMonth);
    }
}
