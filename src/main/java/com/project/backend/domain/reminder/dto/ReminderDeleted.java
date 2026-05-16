package com.project.backend.domain.reminder.dto;

import com.project.backend.domain.reminder.enums.DeletedType;
import com.project.backend.domain.reminder.enums.TargetType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ReminderDeleted(
        Long exceptionId,
        List<Long> memberId,
        LocalDateTime occurrenceTime,
        Long targetId,
        TargetType targetType,
        DeletedType deletedType
) {}