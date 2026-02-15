package com.project.backend.domain.common.reminder;

import com.project.backend.domain.common.plan.PlanEventFactory;
import com.project.backend.domain.reminder.enums.ChangeType;
import com.project.backend.domain.reminder.enums.DeletedType;
import com.project.backend.domain.reminder.enums.ExceptionChangeType;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.reminder.listener.ReminderListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReminderEventBridge {

    private final ReminderListener reminderListener;

    public void handlePlanChanged (
            Long targetId,
            TargetType targetType,
            Long memberId,
            String title,
            Boolean isRecurring,
            LocalDateTime startTime,
            ChangeType changeType
    ) {
        reminderListener.onPlanChanged(PlanEventFactory.toPlanChanged(
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
        reminderListener.onRecurrenceExceptionChanged(PlanEventFactory.toRecurrenceExceptionChanged(
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

    public void handleRecurrenceEnded(Long targetId, TargetType targetType, LocalDateTime startTime) {
        reminderListener.onRecurrenceEnded(PlanEventFactory.toEventRecurrenceEnded(
                targetId,
                targetType,
                startTime
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
        reminderListener.onReminderDeleted(PlanEventFactory.toReminderDeleted(
                exceptionId,
                memberId,
                occurrenceTime,
                eventId,
                targetType,
                deletedType
        ));
    }
}
