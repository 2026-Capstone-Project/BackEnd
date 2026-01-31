package com.project.backend.domain.event.strategy.generator;

import com.project.backend.global.recurrence.RecurrenceRule;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DailyGenerator implements Generator {

    // 매일 매일 반복
    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceRule rule) {

        // 반복 일
        int interval = rule.getIntervalValue();
        return current.plusDays(interval);
    }
}
