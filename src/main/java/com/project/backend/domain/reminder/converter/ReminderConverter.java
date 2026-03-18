package com.project.backend.domain.reminder.converter;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.reminder.dto.PlanChanged;
import com.project.backend.domain.reminder.dto.RecurrenceExceptionChanged;
import com.project.backend.domain.reminder.dto.ReminderDeleted;
import com.project.backend.domain.reminder.dto.ReminderSource;
import com.project.backend.domain.reminder.dto.response.ReminderResDTO;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.enums.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReminderConverter {

    public static Reminder toReminder(
            ReminderSource source,
            Member member,
            Long exceptionId,
            LifecycleStatus lifecycleStatus,
            ReminderRole role
    ) {
        return Reminder.builder()
                .title(source.title())
                .occurrenceTime(source.occurrenceTime())
                .targetType(source.targetType())
                .targetId(source.targetId())
                .recurrenceExceptionId(exceptionId)
                .interactionStatus(InteractionStatus.PENDING)
                .lifecycleStatus(lifecycleStatus)
                .role(role)
                .member(member)
                .build();
    }

    public static Reminder toReminderWithOccurrence(
            ReminderSource source,
            Member member,
            LocalDateTime occurrenceTime,
            LifecycleStatus lifecycleStatus,
            ReminderRole role,
            Long recurrenceExceptionId

    ) {
        return Reminder.builder()
                .title(source.title())
                .occurrenceTime(occurrenceTime)
                .targetType(source.targetType())
                .targetId(source.targetId())
                .recurrenceExceptionId(recurrenceExceptionId)
                .interactionStatus(InteractionStatus.PENDING)
                .lifecycleStatus(lifecycleStatus)
                .role(role)
                .member(member)
                .build();
    }

    public static ReminderResDTO.DetailRes toDetailRes(
            Reminder reminder,
            int minutes,
            String title,
            String message
    ) {
        return ReminderResDTO.DetailRes.builder()
                .id(reminder.getId())
                .reminderTime(LocalTime.of(minutes / 60, minutes % 60))
                .time(reminder.getOccurrenceTime().toLocalTime())
                .title(title)
                .message(message)
                .build();
    }

    public static ReminderSource toReminderSource(
            Long targetId,
            TargetType targetType,
            String title,
            LocalDateTime occurrenceTime,
            Boolean isRecurring
    ) {
        return com.project.backend.domain.reminder.dto.ReminderSource.builder()
                .targetId(targetId)
                .targetType(targetType)
                .title(title)
                .occurrenceTime(occurrenceTime)
                .isRecurring(isRecurring)
                .build();
    }

    public static ReminderSource toReminderSource(
            ReminderSource base,
            LocalDateTime occurrenceTime
    ){
        return ReminderSource.builder()
                .targetId(base.targetId())
                .targetType(base.targetType())
                .title(base.title())
                .occurrenceTime(occurrenceTime)
                .isRecurring(base.isRecurring())
                .build();
    }

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
                .isRecurring(isRecurring)
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
                .occurrenceTime(occurrenceTime)
                .targetId(targetId)
                .targetType(targetType)
                .deletedType(deletedType)
                .build();
    }
}
