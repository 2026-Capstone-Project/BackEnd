package com.project.backend.domain.suggestion.vo.fingerprint;

import com.project.backend.domain.event.entity.Event;

import java.time.LocalDateTime;

public record EventFingerPrint(
        LocalDateTime startTime,
        LocalDateTime endTime,
        Boolean isAllDay,          // [CHANGED-FP-1] allDay도 제안 문구/패턴에 영향 가능
        Long recurrenceGroupId     // 단일/반복 전환 감지
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
