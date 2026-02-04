package com.project.backend.domain.reminder.service.command;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.entity.ReminderSource;

import java.util.List;

public interface ReminderCommandService {

    void createReminder(ReminderSource source, Member member);

    List<Reminder> findActiveReminders();

    void refreshReminder(Reminder reminder);
}
