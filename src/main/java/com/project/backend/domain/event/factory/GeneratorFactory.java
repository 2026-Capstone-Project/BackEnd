package com.project.backend.domain.event.factory;

import com.project.backend.domain.event.strategy.generator.*;
import com.project.backend.global.recurrence.RecurrenceRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * 반복 패턴에 따라 알맞은 생성 전략을 주입하는 Factory
 */

@Component
@RequiredArgsConstructor
public class GeneratorFactory {

    private final DefaultGenerator defaultGenerator;
    private final DailyGenerator dailyGenerator;
    private final WeeklyGenerator weeklyGenerator;
    private final MonthlyGenerator monthlyGenerator;
    private final YearlyGenerator yearlyGenerator;

    public Generator getGenerator(RecurrenceRule rule) {

        // 단발성 이벤트 또는 할 일
        if (rule == null) {
            return defaultGenerator;
        }

        return switch (rule.getFrequency()) {
            case DAILY -> dailyGenerator;
            case WEEKLY -> weeklyGenerator;
            case MONTHLY -> monthlyGenerator;
            case YEARLY -> yearlyGenerator;
            case NONE -> defaultGenerator;
        };
    }
}
