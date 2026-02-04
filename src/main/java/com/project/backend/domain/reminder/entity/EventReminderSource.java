package com.project.backend.domain.reminder.entity;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.reminder.enums.TargetType;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class EventReminderSource implements ReminderSource{

    private final Event event;
    private final LocalDateTime nextOccurrence;

    @Override
    public Long getTargetId() {
        return event.getId();
    }

    @Override
    public TargetType getTargetType() {
        return TargetType.EVENT;
    }

    @Override
    public LocalDateTime getNextOccurrence() {
        return nextOccurrence;
    }

    @Override
    public String getTitle() {
        return event.getTitle();
    }
}
