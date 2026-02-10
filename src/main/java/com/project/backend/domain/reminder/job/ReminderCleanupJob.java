package com.project.backend.domain.reminder.job;

import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class ReminderCleanupJob {

    private final ReminderRepository reminderRepository;

    public void run() {
        reminderRepository.deleteByLifecycleStatus(LifecycleStatus.TERMINATED);
    }
}
