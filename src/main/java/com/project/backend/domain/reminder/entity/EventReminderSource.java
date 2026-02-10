package com.project.backend.domain.reminder.entity;

import com.project.backend.domain.reminder.enums.TargetType;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Builder
@RequiredArgsConstructor
public class EventReminderSource implements ReminderSource{

    private final Long eventId;
    private final String title;
    private final LocalDateTime occurrenceTime;
    private final Boolean isrRecurring;

    @Override
    public Long getTargetId() {
        return eventId;
    }

    @Override
    public TargetType getTargetType() {
        return TargetType.EVENT;
    }

    @Override
    public LocalDateTime getOccurrenceTime() {
        return occurrenceTime;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public Boolean getIsRecurring() {
        return isrRecurring;
    }
}
