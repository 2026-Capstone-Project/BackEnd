package com.project.backend.domain.reminder.handler;

import com.project.backend.domain.event.dto.RecurrenceExceptionChanged;
import com.project.backend.domain.reminder.entity.ReminderSource;
import com.project.backend.domain.reminder.provider.ReminderSourceProvider;
import com.project.backend.domain.reminder.service.command.ReminderCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionReminderHandler {

    private final ReminderCommandService reminderCommandService;
    private final ReminderSourceProvider reminderSourceProvider;

    @Transactional
    public void handle(RecurrenceExceptionChanged rec) {
        ReminderSource rs = reminderSourceProvider
                .getEventReminderSource(rec.eventId(), rec.title(), rec.occurrenceTime(), rec.isrRecurring());
        switch (rec.changeType()) {
            case UPDATED_THIS, DELETED_THIS -> {
                reminderCommandService.refreshIfOccurrenceInvalidated(rs, rec.exceptionId());
            }
            case UPDATE_THIS_AGAIN -> {
                reminderCommandService.syncReminderAfterExceptionUpdate(rs, rec.exceptionId(), rec.memberId());
            }
        }
    }
}
