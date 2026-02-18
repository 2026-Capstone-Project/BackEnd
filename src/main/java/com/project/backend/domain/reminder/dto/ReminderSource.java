package com.project.backend.domain.reminder.dto;

import com.project.backend.domain.reminder.enums.TargetType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReminderSource(
        Long targetId,
        TargetType targetType,
        String title,
        LocalDateTime occurrenceTime,
        Boolean isRecurring
) {
}
