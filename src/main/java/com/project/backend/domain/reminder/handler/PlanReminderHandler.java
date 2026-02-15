package com.project.backend.domain.reminder.handler;

import com.project.backend.domain.event.dto.PlanChanged;
import com.project.backend.domain.reminder.converter.ReminderConverter;
import com.project.backend.domain.reminder.dto.ReminderSource;
import com.project.backend.domain.reminder.service.command.ReminderCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlanReminderHandler {

    private final ReminderCommandService reminderCommandService;

    @Transactional
    public void handle(PlanChanged pc) {
        ReminderSource rs = ReminderConverter.toReminderSource
                (pc.targetId(), pc.targetType(), pc.title(), pc.occurrenceTime(), pc.isrRecurring());
        switch (pc.changeType()){
            case CREATED -> reminderCommandService.createReminder(rs, pc.memberId());
            case UPDATE_SINGLE -> reminderCommandService.updateReminderOfSingle(rs, pc.memberId());
            case UPDATE_ADD_RECURRENCE ->
                    reminderCommandService.updateReminderOfRecurrence(rs, pc.memberId(), pc.occurrenceTime());
        }
    }
}
