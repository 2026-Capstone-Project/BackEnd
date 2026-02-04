package com.project.backend.domain.reminder.dto.response;

import lombok.Builder;

import java.time.LocalTime;

public class ReminderResDTO {

    @Builder
    public record DetailRes(
            Long id,
            LocalTime reminderTime,
            LocalTime time,
            String title,
            String message
    ) {
    }
}
