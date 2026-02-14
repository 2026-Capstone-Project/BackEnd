package com.project.backend.domain.reminder.handler;

import com.project.backend.domain.event.dto.RecurrenceExceptionChanged;
import com.project.backend.domain.reminder.converter.ReminderConverter;
import com.project.backend.domain.reminder.dto.ReminderSource;
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

    @Transactional
    public void handle(RecurrenceExceptionChanged rec) {
        ReminderSource rs = ReminderConverter.toReminderSource(
                rec.eventId(), rec.targetType(), rec.title(), rec.occurrenceTime(), rec.isrRecurring()
        );
        switch (rec.changeType()) {
            case UPDATED_THIS -> reminderCommandService.refreshIfOccurrenceInvalidated(
                    rs, rec.exceptionId(), false
            );

            case UPDATE_THIS_AGAIN -> reminderCommandService.syncReminderAfterExceptionUpdate(
                    rs, rec.exceptionId(), rec.memberId()
            );

            case DELETED_THIS -> reminderCommandService.refreshIfOccurrenceInvalidated(
                    rs, rec.exceptionId(), true
            );
        }
    }
}
