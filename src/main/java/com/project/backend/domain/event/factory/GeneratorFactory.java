package com.project.backend.domain.event.factory;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.strategy.generator.*;
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

    public Generator getGenerator(RecurrenceGroup rg) {

        // 단발성 이벤트
        if (rg == null) {
            return defaultGenerator;
        }

        return switch (rg.getFrequency()) {
            case DAILY -> dailyGenerator;
            case WEEKLY -> weeklyGenerator;
            case MONTHLY -> monthlyGenerator;
            case YEARLY -> yearlyGenerator;
            case NONE -> defaultGenerator;
        };
    }
}
