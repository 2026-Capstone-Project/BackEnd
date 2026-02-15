package com.project.backend.domain.suggestion.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeekEpochUtil {
    public static long toEpochWeek(LocalDate date) {
        // ISO 기준: 주 시작 = 월요일
        LocalDate weekStart =
                date.with(WeekFields.ISO.dayOfWeek(), 1);

        // 1970-01-05는 ISO 기준 월요일
        LocalDate epochStart = LocalDate.of(1970, 1, 5);

        return ChronoUnit.WEEKS.between(epochStart, weekStart);
    }
}
