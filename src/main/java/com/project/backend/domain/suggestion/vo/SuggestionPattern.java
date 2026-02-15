package com.project.backend.domain.suggestion.vo;

import lombok.Builder;

import java.time.DayOfWeek;
import java.util.Set;

@Builder
public record SuggestionPattern(
        Integer dayDiff,
        Integer weekDiff,
        Integer monthDiff,
        Set<DayOfWeek> dayOfWeekSet,
        Set<Integer> dayOfMonthSet
) {
}
