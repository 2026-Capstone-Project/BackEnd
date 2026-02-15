package com.project.backend.domain.suggestion.detector.vo;

import java.util.Set;

public record MonthlyPattern(
        int monthDiff,
        Set<Integer> dayOfMonthSet
) {
}