package com.project.backend.domain.suggestion.vo.fingerprint;

import com.project.backend.domain.event.entity.Event;

import java.time.LocalDateTime;

public record EventFingerPrint(
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isAllDay,
        Long recurrenceGroupId
) {
    public static EventFingerPrint from(Event event) {
        return new EventFingerPrint(
                event.getStartTime(),
                event.getEndTime(),
                event.getIsAllDay(),
                event.getRecurrenceGroup() != null ? event.getRecurrenceGroup().getId() : null
        );
    }
}
