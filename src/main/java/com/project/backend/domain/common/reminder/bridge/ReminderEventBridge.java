package com.project.backend.domain.common.reminder.bridge;

import com.project.backend.domain.reminder.converter.ReminderConverter;
import com.project.backend.domain.reminder.enums.ChangeType;
import com.project.backend.domain.reminder.enums.DeletedType;
import com.project.backend.domain.reminder.enums.ExceptionChangeType;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.reminder.handler.ExceptionReminderHandler;
import com.project.backend.domain.reminder.handler.PlanReminderHandler;
import com.project.backend.domain.reminder.handler.ReminderDeletedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReminderEventBridge {

    private final PlanReminderHandler planReminderHandler;
    private final ExceptionReminderHandler exceptionReminderHandler;
    private final ReminderDeletedHandler reminderDeletedHandler;

    public void handlePlanChanged (
            Long targetId,
            TargetType targetType,
            Long memberId,
            String title,
            Boolean isRecurring,
            LocalDateTime startTime,
            ChangeType changeType
    ) {
        planReminderHandler.handle(ReminderConverter.toPlanChanged(
                targetId,
                targetType,
                memberId,
                title,
                isRecurring,
                startTime,
                changeType
        ));
    }

    public void handleExceptionChanged (
            Long exceptionId,
            Long targetId,
            TargetType targetType,
            Long memberId,
            String title,
            LocalDateTime occurrenceTime,
            ExceptionChangeType changeType
    ) {
        exceptionReminderHandler.handle(ReminderConverter.toRecurrenceExceptionChanged(
                exceptionId,
                targetId,
                targetType,
                memberId,
                title,
                true,
                occurrenceTime,
                changeType
        ));
    }

    public void handleReminderDeleted(
            Long exceptionId,
            Long memberId,
            LocalDateTime occurrenceTime,
            Long eventId,
            TargetType targetType,
            DeletedType deletedType
    ) {
        reminderDeletedHandler.handle(ReminderConverter.toReminderDeleted(
                exceptionId,
                memberId,
                occurrenceTime,
                eventId,
                targetType,
                deletedType
        ));
    }
}
