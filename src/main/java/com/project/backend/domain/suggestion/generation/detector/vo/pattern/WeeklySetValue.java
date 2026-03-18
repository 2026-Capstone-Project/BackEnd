package com.project.backend.domain.suggestion.generation.detector.vo.pattern;

import java.time.DayOfWeek;
import java.util.Set;

public record WeeklySetValue(
        int weekDiff,
        Set<DayOfWeek> dayOfWeekSet
) implements PatternValue {

}
