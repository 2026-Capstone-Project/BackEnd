package com.project.backend.domain.reminder.schedular;

import com.project.backend.domain.reminder.job.ReminderCleanupJob;
import com.project.backend.domain.reminder.job.ReminderGenerationJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyMaintenanceScheduler {

    private final ReminderGenerationJob reminderGenerationJob;
    private final ReminderCleanupJob reminderCleanupJob;

    @Scheduled(cron = "0 0 0 * * *")
    public void dailyRun() {
        log.info("Daily Maintenance Job Started");
        reminderGenerationJob.run();
        reminderCleanupJob.run();
    }

}
