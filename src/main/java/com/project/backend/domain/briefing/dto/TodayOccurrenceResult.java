package com.project.backend.domain.briefing.dto;

import com.project.backend.domain.reminder.enums.TargetType;

import java.time.LocalTime;

public record TodayOccurrenceResult (
    boolean hasToday,
    String title,
    LocalTime time,
    TargetType targetType
) {
        // 오늘 브리핑 대상이 아닌경우
        public static TodayOccurrenceResult none() {
            return new TodayOccurrenceResult(false, null,null, null);
        }

        // 오늘 브리핑 대상인경우
        public static TodayOccurrenceResult of(String title, LocalTime time, TargetType targetType) {
            return new TodayOccurrenceResult(true, title, time, targetType);
        }

}
