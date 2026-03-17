package com.project.backend.domain.suggestion.generation.detector.vo;

import java.time.DayOfWeek;
import java.util.Set;

public record WeeklyPattern(
        int weekDiff,
        Set<DayOfWeek> dayOfWeekSet
) {
}
