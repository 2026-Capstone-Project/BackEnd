package com.project.backend.domain.suggestion.generation.detector.vo.pattern;

public sealed interface PatternValue
        permits IntervalValue, WeeklySetValue, MonthlySetValue {
}
