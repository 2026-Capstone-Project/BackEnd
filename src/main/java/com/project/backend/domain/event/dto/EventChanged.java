package com.project.backend.domain.event.dto;

import com.project.backend.domain.reminder.enums.ChangeType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record EventChanged (
        Long eventId,
        Long memberId,
        String title,
        Boolean isrRecurring,
        // 생성 / 수정에서만 의미 있음
        LocalDateTime occurrenceTime,
        ChangeType changeType
) {
}
