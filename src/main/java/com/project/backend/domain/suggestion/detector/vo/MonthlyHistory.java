package com.project.backend.domain.suggestion.detector.vo;

import java.util.List;
import java.util.Set;

public record MonthlyHistory(
        List<Integer> monthDiff,
        List<Set<Integer>> dayOfMonthSet
) {
}
