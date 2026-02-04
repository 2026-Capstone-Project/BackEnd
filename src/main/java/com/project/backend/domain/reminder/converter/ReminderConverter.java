package com.project.backend.domain.reminder.converter;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.reminder.dto.response.ReminderResDTO;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.entity.ReminderSource;
import com.project.backend.domain.reminder.enums.InteractionStatus;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.enums.TargetType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReminderConverter {

    public static Reminder toReminder(ReminderSource source, Member member, LifecycleStatus lifecycleStatus) {
        return Reminder.builder()
                .title(source.getTitle())
                .occurrenceTime(source.getNextOccurrence())
                .targetType(TargetType.EVENT)
                .targetId(source.getTargetId())
                .interactionStatus(InteractionStatus.PENDING)
                .lifecycleStatus(lifecycleStatus)
                .member(member)
                .build();
    }

    public static ReminderResDTO.DetailRes toDetailRes(Reminder reminder, int minutes, String message) {
        return ReminderResDTO.DetailRes.builder()
                .id(reminder.getId())
                .reminderTime(LocalTime.ofSecondOfDay(!(minutes >= 60) ? minutes : minutes / 60))
                .time(reminder.getOccurrenceTime().toLocalTime())
                .title(reminder.getTitle())
                .message(message)
                .build();
    }
}
