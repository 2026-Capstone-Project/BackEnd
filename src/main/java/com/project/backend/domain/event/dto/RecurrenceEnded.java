package com.project.backend.domain.event.dto;

import com.project.backend.domain.reminder.enums.TargetType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RecurrenceEnded(
        Long targetId,
        TargetType targetType,
        LocalDateTime startTime // 새로 생성된 일정 + 반복 의 startTime
) {
}
