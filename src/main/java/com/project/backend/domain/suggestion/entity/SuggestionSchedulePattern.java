package com.project.backend.domain.suggestion.entity;

import com.project.backend.domain.suggestion.enums.RecurrencePatternType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.util.Set;

@Embeddable
public class SuggestionSchedulePattern {

    @Enumerated(EnumType.STRING)
    private RecurrencePatternType patternType;

    private Integer dayDiff;
    private Integer weekDiff;
    private Integer monthDiff;

    @ElementCollection
    private Set<DayOfWeek> dayOfWeekSet;

    @ElementCollection
    private Set<Integer> dayOfMonthSet;
}
