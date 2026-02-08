package com.project.backend.domain.reminder.service.command;

import com.project.backend.domain.event.dto.RecurrenceEnded;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.entity.ReminderSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReminderCommandService {

    void createReminder(ReminderSource rs, Long memberId);

    void createSingleOverrideReminder(Long eventId, Long memberId, LocalDateTime occurrenceTime, String title);

    List<Reminder> findActiveReminders();

    void refreshIfExpired(Reminder reminder);

    void refreshDueToUpdate(Reminder reminder);

    void refreshIfOccurrenceInvalidated(ReminderSource rs, Long exceptionId);

    void updateReminderOfSingle(ReminderSource rs, Long memberId);

    void updateReminderOfRecurrence(ReminderSource rs, Long memberId, LocalDateTime occurrenceTime);

    void cleanupBaseReminderOnUpdate(RecurrenceEnded re);

    void deleteReminderOfSingle(ReminderSource rs);

    void deleteReminderOfThisAndFollowings(ReminderSource rs, LocalDate occurrenceDate);

    void deleteReminderOfAll(ReminderSource rs, LocalDateTime occurrenceTime);
}
