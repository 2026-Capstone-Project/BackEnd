package com.project.backend.domain.event.dto;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ResolvedOccurrence (
        LocalDateTime occurrenceDate,
        Event event,
        RecurrenceException exception
) {
}
