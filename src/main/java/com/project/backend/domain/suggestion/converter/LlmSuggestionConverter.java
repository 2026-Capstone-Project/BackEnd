package com.project.backend.domain.suggestion.converter;

import com.project.backend.domain.suggestion.detector.vo.DetectionResult;
import com.project.backend.domain.suggestion.detector.vo.pattern.IntervalValue;
import com.project.backend.domain.suggestion.detector.vo.pattern.MonthlySetValue;
import com.project.backend.domain.suggestion.vo.SuggestionPattern;
import com.project.backend.domain.suggestion.detector.vo.pattern.WeeklySetValue;
import com.project.backend.domain.suggestion.dto.request.SuggestionReqDTO;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.vo.SuggestionCandidate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LlmSuggestionConverter {

    public static SuggestionReqDTO.LlmSuggestionDetail toLlmSuggestionDetail(
            SuggestionCandidate baseCandidate,
            DetectionResult dr
    ) {
        return switch (dr.patternType()) {
            case N_INTERVAL -> toInterval(baseCandidate, dr);
            case WEEKLY_SET -> toWeeklySet(baseCandidate, dr);
            case MONTHLY_DAY -> toMonthlyDay(baseCandidate, dr);
        };
    }

    private static SuggestionReqDTO.LlmSuggestionDetail.LlmSuggestionDetailBuilder baseBuilder(
            SuggestionCandidate baseCandidate,
            DetectionResult dr
    ) {
        return SuggestionReqDTO.LlmSuggestionDetail.builder()
                .eventId(baseCandidate.id())
                .title(baseCandidate.title())
                .start(baseCandidate.start())
                .category(Category.EVENT)
                .patternType(dr.patternType())
                .stableType(dr.stableType());
    }

    private static SuggestionReqDTO.LlmSuggestionDetail toInterval(
            SuggestionCandidate baseCandidate,
            DetectionResult dr
    ) {
        IntervalValue primary = (IntervalValue) dr.primary();
        IntervalValue secondary = (IntervalValue) dr.secondary();

        return baseBuilder(baseCandidate, dr)
                .primaryPattern(intervalPattern(primary))
                .secondaryPattern(intervalPattern(secondary))
                .build();
    }

    private static SuggestionReqDTO.LlmSuggestionDetail toWeeklySet(
            SuggestionCandidate baseCandidate,
            DetectionResult dr
    ) {
        WeeklySetValue primary = (WeeklySetValue) dr.primary();
        WeeklySetValue secondary = (WeeklySetValue) dr.secondary();

        return baseBuilder(baseCandidate, dr)
                .primaryPattern(weeklySetPattern(primary))
                .secondaryPattern(weeklySetPattern(secondary))
                .build();
    }

    private static SuggestionReqDTO.LlmSuggestionDetail toMonthlyDay(
            SuggestionCandidate baseCandidate,
            DetectionResult dr
    ) {
        MonthlySetValue primary = (MonthlySetValue) dr.primary();
        MonthlySetValue secondary = (MonthlySetValue) dr.secondary();

        return baseBuilder(baseCandidate, dr)
                .primaryPattern(monthlyDayPattern(primary))
                .secondaryPattern(monthlyDayPattern(secondary))
                .build();
    }

    private static SuggestionPattern intervalPattern(IntervalValue v) {
        if (v == null) return null;

        return SuggestionPattern.builder()
                .dayDiff(v.dayDiff())
                .build();
    }

    private static SuggestionPattern weeklySetPattern(WeeklySetValue v) {
        if (v == null) return null;

        return SuggestionPattern.builder()
                .weekDiff(v.weekDiff())
                .dayOfWeekSet(v.dayOfWeekSet())
                .build();
    }

    private static SuggestionPattern monthlyDayPattern(MonthlySetValue v) {
        if (v == null) return null;

        return SuggestionPattern.builder()
                .monthDiff(v.monthDiff())
                .dayOfMonthSet(v.dayOfMonthSet())
                .build();
    }
}
