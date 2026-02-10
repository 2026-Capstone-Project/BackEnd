package com.project.backend.domain.todo.dto;

import com.project.backend.domain.reminder.enums.ChangeType;

public record TodoChanged (
        Long targetId,
        Long memberId,
        ChangeType changeType
) {
}