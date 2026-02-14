package com.project.backend.domain.suggestion.detector.vo;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public record WeeklyHistory(
        List<Integer> weekDiff,
        List<Set<DayOfWeek>> dayOfWeekSet
) {
}
