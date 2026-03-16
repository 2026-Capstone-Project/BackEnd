package com.project.backend.domain.suggestion.invalidation.fingerprint;

import com.project.backend.domain.event.entity.Event;

import java.time.LocalDateTime;

public record EventFingerPrint(
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isAllDay,
        Long recurrenceGroupId
) implements PlanFingerprint {
    public static EventFingerPrint from(Event event) {
        return new EventFingerPrint(
                event.getStartTime(),
                event.getEndTime(),
                event.getIsAllDay(),
                event.getRecurrenceGroup() != null ? event.getRecurrenceGroup().getId() : null
        );
    }
}
