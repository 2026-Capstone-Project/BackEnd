package com.project.backend.domain.suggestion.detector.vo.pattern;

public sealed interface PatternValue
        permits IntervalValue, WeeklySetValue, MonthlySetValue {
}
