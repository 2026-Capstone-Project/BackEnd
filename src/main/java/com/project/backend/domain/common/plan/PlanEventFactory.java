package com.project.backend.domain.common.plan;

import com.project.backend.domain.event.dto.PlanChanged;
import com.project.backend.domain.event.dto.RecurrenceExceptionChanged;
import com.project.backend.domain.reminder.dto.ReminderDeleted;
import com.project.backend.domain.reminder.enums.ChangeType;
import com.project.backend.domain.reminder.enums.DeletedType;
import com.project.backend.domain.reminder.enums.ExceptionChangeType;
import com.project.backend.domain.reminder.enums.TargetType;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class PlanEventFactory {

    public static PlanChanged toPlanChanged(
            Long targetId,
            TargetType targetType,
            Long memberId,
            String title,
            Boolean isRecurring,
            LocalDateTime occurrenceTime,
            ChangeType changeType) {
        return PlanChanged.builder()
                .targetId(targetId)
                .targetType(targetType)
                .memberId(memberId)
                .title(title)
                .isrRecurring(isRecurring)
                .occurrenceTime(occurrenceTime)
                .changeType(changeType)
                .build();
    }

    public static RecurrenceExceptionChanged toRecurrenceExceptionChanged (
            Long exceptionId,
            Long eventId,
            TargetType targetType,
            Long memberId,
            String title,
            Boolean isRecurring,
            LocalDateTime occurrenceTime,
            ExceptionChangeType changeType
    ) {
        return RecurrenceExceptionChanged.builder()
                .exceptionId(exceptionId)
                .eventId(eventId)
                .targetType(targetType)
                .memberId(memberId)
                .title(title)
                .isrRecurring(isRecurring)
                .occurrenceTime(occurrenceTime)
                .changeType(changeType)
                .build();
    }

    public static ReminderDeleted toReminderDeleted(
            Long exceptionId,
            Long memberId,
            LocalDateTime occurrenceTime,
            Long targetId,
            TargetType targetType,
            DeletedType deletedType
    ) {
        return ReminderDeleted.builder()
                .exceptionId(exceptionId)
                .memberId(memberId)
                .occurenceTime(occurrenceTime)
                .targetId(targetId)
                .targetType(targetType)
                .deletedType(deletedType)
                .build();
    }
}
