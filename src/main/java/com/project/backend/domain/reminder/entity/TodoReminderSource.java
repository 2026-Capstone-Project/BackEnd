package com.project.backend.domain.reminder.entity;

import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.todo.entity.Todo;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class TodoReminderSource implements ReminderSource {

    private final Todo todo;
    private final LocalDateTime nextOccurrence;


    @Override
    public Long getTargetId() {
        return todo.getId();
    }

    @Override
    public TargetType getTargetType() {
        return TargetType.TODO;
    }

    @Override
    public LocalDateTime getNextOccurrence() {
        return nextOccurrence;
    }

    @Override
    public String getTitle() {
        return todo.getTitle();
    }
}
