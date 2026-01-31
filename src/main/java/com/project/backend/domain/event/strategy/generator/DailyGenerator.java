package com.project.backend.domain.event.strategy.generator;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DailyGenerator implements Generator {

    // 매일 매일 반복
    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceGroup rg) {

        // 반복 일
        int interval = rg.getIntervalValue();
        return current.plusDays(interval);
    }
}
