package com.project.backend.domain.suggestion.generation.detector.vo;

import java.util.List;

public record RecurrencePreprocessResult(
        List<Integer> dayDiff,
        WeeklyHistory weeklyHistory,
        MonthlyHistory monthlyHistory
) {
}
