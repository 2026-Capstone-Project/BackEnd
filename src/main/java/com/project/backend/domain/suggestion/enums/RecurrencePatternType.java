package com.project.backend.domain.suggestion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecurrencePatternType {
    N_INTERVAL(1),
    WEEKLY_SET(2),
    MONTHLY_DAY(2),
    ;

    private final int score;
}
