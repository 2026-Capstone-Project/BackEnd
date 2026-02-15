package com.project.backend.domain.reminder.service.command;

import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.dto.ReminderSource;
import com.project.backend.domain.reminder.enums.TargetType;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderCommandService {

    void createReminder(ReminderSource rs, Long memberId);

    void createSingleOverrideReminder
            (Long eventId, Long memberId, LocalDateTime occurrenceTime, String title, Long exceptionId);

    List<Reminder> findActiveReminders();

    void refreshIfExpired(Reminder reminder);

    void refreshIfOccurrenceInvalidated(ReminderSource rs, Long exceptionId, Boolean isSkip);

    void updateReminderOfSingle(ReminderSource rs, Long memberId);

    void updateReminderOfRecurrence(ReminderSource rs, Long memberId, LocalDateTime occurrenceTime);

    void syncReminderAfterExceptionUpdate(ReminderSource rs, Long exceptionId, Long memberId);

    void deleteReminderOfSingle(Long targetId, TargetType targetType, LocalDateTime occurrenceTime);

    void deleteReminderOfThisAndFollowings(Long targetId, TargetType targetType, LocalDateTime occurrenceTime);

    void deleteReminderOfAll(Long targetId, TargetType targetType, LocalDateTime occurrenceTime);
}
