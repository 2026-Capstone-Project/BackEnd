package com.project.backend.domain.reminder.handler;

import com.project.backend.domain.reminder.dto.ReminderDeleted;
import com.project.backend.domain.reminder.service.command.ReminderCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReminderDeletedHandler {

    private final ReminderCommandService reminderCommandService;

    public void handle(ReminderDeleted rd) {
        switch (rd.deletedType()){
            case DELETED_SINGLE -> reminderCommandService.
                    deleteReminderOfSingle(rd.targetId(), rd.targetType(), rd.occurrenceTime());
            case DELETED_THIS_AND_FOLLOWING ->
                    reminderCommandService.deleteReminderOfThisAndFollowings(
                            rd.targetId(), rd.targetType(), rd.occurrenceTime());
            case DELETED_ALL ->
                    reminderCommandService.deleteReminderOfAll(rd.targetId(), rd.targetType());
        }
    }
}
