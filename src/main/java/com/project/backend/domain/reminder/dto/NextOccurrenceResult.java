package com.project.backend.domain.reminder.dto;

import java.time.LocalDateTime;

public record NextOccurrenceResult(
        boolean hasNext,
        LocalDateTime nextTime
) {
    // 다음 계산된 날짜가 없을 때 or 단일 일정일 경우
    public static NextOccurrenceResult none() {
        return new NextOccurrenceResult(false, null);
    }

    // 다음 계산된 날짜가 있을 경우
    public static NextOccurrenceResult of(LocalDateTime nextTime) {
        return new NextOccurrenceResult(true, nextTime);
    }
}

