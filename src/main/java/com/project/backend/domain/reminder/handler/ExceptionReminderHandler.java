package com.project.backend.domain.reminder.handler;

import com.project.backend.domain.event.dto.RecurrenceExceptionChanged;
import com.project.backend.domain.reminder.entity.ReminderSource;
import com.project.backend.domain.reminder.provider.ReminderSourceProvider;
import com.project.backend.domain.reminder.service.command.ReminderCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionReminderHandler {

    private final ReminderCommandService reminderCommandService;
    private final ReminderSourceProvider reminderSourceProvider;

    public void handle(RecurrenceExceptionChanged rec) {
        ReminderSource rs = reminderSourceProvider
                .getEventReminderSource(rec.eventId(), rec.title(), rec.occurrenceTime(), rec.isrRecurring());
        switch (rec.changeType()) {
            case UPDATED_THIS, DELETED_THIS -> {
                reminderCommandService.refreshIfOccurrenceInvalidated(rs, rec.exceptionId());
            }
        }
    }
}
