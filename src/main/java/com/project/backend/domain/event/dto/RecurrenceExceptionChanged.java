package com.project.backend.domain.event.dto;

import com.project.backend.domain.reminder.enums.ExceptionChangeType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RecurrenceExceptionChanged(
        Long exceptionId,
        Long eventId,
        Long memberId,
        String title,
        LocalDateTime occurrenceTime,
        Boolean isrRecurring,
        ExceptionChangeType changeType
) {}