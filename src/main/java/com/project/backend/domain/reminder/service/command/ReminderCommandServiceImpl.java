package com.project.backend.domain.reminder.service.command;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.reminder.converter.ReminderConverter;
import com.project.backend.domain.reminder.dto.NextOccurrenceResult;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.entity.ReminderSource;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.repository.ReminderRepository;
import com.project.backend.domain.reminder.service.OccurrenceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReminderCommandServiceImpl implements ReminderCommandService {

    private final ReminderRepository reminderRepository;
    private final OccurrenceProvider occurrenceProvider;

    @Override
    public void createReminder(ReminderSource source, Member member) {
        LocalDateTime now = LocalDateTime.now();

        LifecycleStatus lifecycleStatus =
                source.getNextOccurrence().isBefore(now)
                ? LifecycleStatus.TERMINATED
                : LifecycleStatus.ACTIVE;

        Reminder reminder = ReminderConverter.toReminder(source, member, lifecycleStatus);
        reminderRepository.save(reminder);
    }

    @Override
    public List<Reminder> findActiveReminders() {
        return reminderRepository.findAllByLifecycleStatus(LifecycleStatus.ACTIVE);
    }

    @Override
    public void refreshReminder(Reminder reminder) {
        LocalDateTime now = LocalDateTime.now();
        if (!reminder.getOccurrenceTime().isBefore(now)) {
            return; // 아직 갱신 시점 아님 30
        }

        NextOccurrenceResult result = occurrenceProvider.getNextOccurrence(reminder);

        if (!result.hasNext()) {
            reminder.terminate();
            return;
        }

        reminder.updateOccurrence(result.nextTime());
    }
}
