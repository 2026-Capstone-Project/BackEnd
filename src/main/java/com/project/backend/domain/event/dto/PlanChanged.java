package com.project.backend.domain.event.dto;

import com.project.backend.domain.reminder.enums.ChangeType;
import com.project.backend.domain.reminder.enums.TargetType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PlanChanged(
        Long targetId,
        TargetType targetType,
        Long memberId,
        String title,
        Boolean isrRecurring,
        // 생성 / 수정에서만 의미 있음
        LocalDateTime occurrenceTime,
        ChangeType changeType
) {
}
