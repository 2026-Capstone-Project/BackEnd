package com.project.backend.domain.suggestion.detector.vo;

import com.project.backend.domain.suggestion.detector.vo.pattern.IntervalValue;
import com.project.backend.domain.suggestion.detector.vo.pattern.MonthlySetValue;
import com.project.backend.domain.suggestion.detector.vo.pattern.PatternValue;
import com.project.backend.domain.suggestion.detector.vo.pattern.WeeklySetValue;
import com.project.backend.domain.suggestion.enums.RecurrencePatternType;
import com.project.backend.domain.suggestion.enums.StableType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record DetectionResult(
        RecurrencePatternType patternType,
        StableType stableType,
        PatternValue primary,
        PatternValue secondary
) {

    public static DetectionResult nInterval(
            StableType stableType,
            Integer primary,
            Integer secondary
    ) {
        log.info("interval stable = {}", stableType);
        return new DetectionResult(
                RecurrencePatternType.N_INTERVAL,
                stableType,
                primary != null ? new IntervalValue(primary) : null,
                secondary != null ? new IntervalValue(secondary) : null
        );
    }

    public static DetectionResult weeklySet(
            StableType stableType,
            WeeklyPattern primary,
            WeeklyPattern secondary
    ) {
        log.info("weekly set stable = {}", stableType);
        return new DetectionResult(
                RecurrencePatternType.WEEKLY_SET,
                stableType,
                primary != null ? new WeeklySetValue(primary.weekDiff(), primary.dayOfWeekSet()) : null,
                secondary != null ? new WeeklySetValue(secondary.weekDiff(), secondary.dayOfWeekSet()) : null
        );
    }

    public static DetectionResult monthlyDay(
            StableType stableType,
            MonthlyPattern primary,
            MonthlyPattern secondary
    ) {
        log.info("monthly day stable = {}", stableType);
        return new DetectionResult(
                RecurrencePatternType.MONTHLY_DAY,
                stableType,
                primary != null ? new MonthlySetValue(primary.monthDiff(), primary.dayOfMonthSet()) : null,
                secondary != null ? new MonthlySetValue(secondary.monthDiff(), secondary.dayOfMonthSet()) : null
        );
    }

    public int score() {
        return stableType.getScore() * 10 + patternType.getScore();
    }


}
