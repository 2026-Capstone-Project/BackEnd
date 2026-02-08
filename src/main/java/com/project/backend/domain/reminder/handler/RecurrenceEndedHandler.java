package com.project.backend.domain.reminder.handler;

import com.project.backend.domain.event.dto.RecurrenceEnded;
import com.project.backend.domain.reminder.service.command.ReminderCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurrenceEndedHandler {

    private final ReminderCommandService reminderCommandService;

    public void handle(RecurrenceEnded re) {
        reminderCommandService.cleanupBaseReminderOnUpdate(re);
    }
}
