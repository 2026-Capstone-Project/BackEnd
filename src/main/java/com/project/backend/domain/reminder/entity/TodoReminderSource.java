package com.project.backend.domain.reminder.entity;

import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.todo.entity.Todo;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Builder
@RequiredArgsConstructor
public class TodoReminderSource implements ReminderSource {

    private final Todo todo;
    private final String title;
    private final LocalDateTime occurrenceTime;
    private final Boolean isrRecurring;



    @Override
    public Long getTargetId() {
        return todo.getId();
    }

    @Override
    public TargetType getTargetType() {
        return TargetType.TODO;
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
