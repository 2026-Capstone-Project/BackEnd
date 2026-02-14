package com.project.backend.domain.suggestion.detector.vo.pattern;

import java.util.Set;

public record MonthlySetValue(
        int monthDiff,
        Set<Integer> dayOfMonthSet
) implements PatternValue {

}
