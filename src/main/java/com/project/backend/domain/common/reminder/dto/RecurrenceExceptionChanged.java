package com.project.backend.domain.common.reminder.dto;

import com.project.backend.domain.reminder.enums.ExceptionChangeType;
import com.project.backend.domain.reminder.enums.TargetType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RecurrenceExceptionChanged(
        Long exceptionId,
        Long eventId,
        TargetType targetType,
        Long memberId,
        String title,
        LocalDateTime occurrenceTime,
        Boolean isrRecurring,
        ExceptionChangeType changeType
) {}