package com.project.backend.domain.reminder.job;

import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.service.command.ReminderCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class ReminderGenerationJob {

    private final ReminderCommandService reminderCommandService;

    public void run() {
        List<Reminder> activeReminders = reminderCommandService.findActiveReminders();
        activeReminders.forEach(reminderCommandService::refreshIfExpired);
    }
}
