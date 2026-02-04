package com.project.backend.domain.reminder.entity;

import com.project.backend.domain.reminder.enums.TargetType;

import java.time.LocalDateTime;

public interface ReminderSource {
    Long getTargetId();
    TargetType getTargetType();
    LocalDateTime getNextOccurrence();
    String getTitle();
}
