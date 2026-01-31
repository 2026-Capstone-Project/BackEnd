package com.project.backend.domain.event.strategy.generator;

import com.project.backend.domain.event.strategy.generator.monthlyrule.DayOfMonthRule;
import com.project.backend.domain.event.strategy.generator.monthlyrule.DayOfWeekRule;
import com.project.backend.global.recurrence.RecurrenceRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MonthlyGenerator implements Generator {

    // M달마다 N일
    private final DayOfMonthRule dayOfMonthRule;
    // 매달 N주의 X요일
    private final DayOfWeekRule dayOfWeekRule;

    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceRule rule) {
        // 월별 처리 방식
        return switch (rule.getMonthlyType()) {
            case DAY_OF_MONTH -> dayOfMonthRule.next(current, rule);
            case DAY_OF_WEEK -> dayOfWeekRule.next(current, rule);
        };
    }
}
