package com.project.backend.domain.event.strategy.generator.monthlyrule;

import com.project.backend.global.recurrence.RecurrenceRule;
import com.project.backend.global.recurrence.util.RecurrenceUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DayOfMonthRule implements MonthlyRule {

    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceRule rule) {

        // M달
        int interval = rule.getIntervalValue();
        // X일
        List<Integer> targetDays = RecurrenceUtils.parseDaysOfMonth(rule.getDaysOfMonth());

        // 반복 일자가 비어있으면 시작일의 일자를 기본값으로 사용
        if (targetDays.isEmpty()) {
            targetDays = List.of(current.getDayOfMonth());
        }

        int baseDay = current.getDayOfMonth();

        while (true) {
            int nextTarget = RecurrenceUtils.findNextTarget(baseDay, targetDays);
            int lastDay = current.toLocalDate().lengthOfMonth();

            // 이 달에서 실제로 존재하고, 미래면 OK
            if (nextTarget > baseDay && nextTarget <= lastDay) {
                return current.withDayOfMonth(nextTarget);
            }

            // 아니라면 → 다음 달로 이동, baseDay 리셋
            current = current.plusMonths(interval).withDayOfMonth(1);
            baseDay = 0; // 다음 달에서는 처음부터
        }
    }
}
