package com.project.backend.domain.reminder.converter;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.reminder.dto.response.ReminderResDTO;
import com.project.backend.domain.reminder.entity.EventReminderSource;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.entity.ReminderSource;
import com.project.backend.domain.reminder.entity.TodoReminderSource;
import com.project.backend.domain.reminder.enums.InteractionStatus;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.enums.ReminderRole;
import com.project.backend.domain.todo.entity.Todo;
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
                .title(source.getTitle())
                .occurrenceTime(source.getOccurrenceTime())
                .targetType(source.getTargetType())
                .targetId(source.getTargetId())
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
                .title(source.getTitle())
                .occurrenceTime(occurrenceTime)
                .targetType(source.getTargetType())
                .targetId(source.getTargetId())
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

    public static EventReminderSource toEventReminderSource(
            Long eventId,
            String title,
            LocalDateTime occurrenceTime,
            Boolean isRecurring
    ) {
        return EventReminderSource.builder()
                .eventId(eventId)
                .title(title)
                .occurrenceTime(occurrenceTime)
                .isrRecurring(isRecurring)
                .build();
    }

    public static TodoReminderSource toTodoReminderSource(Todo todo) {
        return TodoReminderSource.builder()
                .todo(todo)
                .occurrenceTime(todo.getStartDate().atTime(todo.getDueTime()))
                .build();
    }
}
