package com.project.backend.domain.event.strategy.generator;

import com.project.backend.global.recurrence.RecurrenceRule;
import com.project.backend.global.recurrence.util.RecurrenceUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class YearlyGenerator implements Generator {

    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceRule rule) {

        // monthOfYear가 null이면 현재 날짜의 월을 사용 (Todo의 경우)
        int monthOfYear = rule.getMonthOfYear() != null
                ? rule.getMonthOfYear()
                : current.getMonthValue();

        List<Integer> targetDays = RecurrenceUtils.parseDaysOfMonth(rule.getDaysOfMonth());

        // targetDays가 null이거나 비어있으면 현재 날짜의 일을 사용 (Todo의 경우)
        if (targetDays == null || targetDays.isEmpty()) {
            // 단순히 intervalValue년 후 같은 월/일 반환
            return current.plusYears(rule.getIntervalValue() != null ? rule.getIntervalValue() : 1);
        }

        // 입력 일
        int baseDay = current.getDayOfMonth();

        // 입력 월
        int baseMonth = current.getMonthValue();

        // 입력 월이 반복 월보다 작다면
        if (baseMonth < monthOfYear) {
            // 반복 월 1일의 전날로 설정
            current = current.withMonth(monthOfYear).withDayOfMonth(1).minusDays(1);
            baseDay = current.getDayOfMonth();
            // 입력 일이 반복 일의 마지막이라면
        } else if (baseDay == targetDays.getLast()) {
            // 다음 년도 변경
            current = current.plusYears(rule.getIntervalValue() != null ? rule.getIntervalValue() : 1);
        }

        // 다음 반복 대상
        int nextTarget = RecurrenceUtils.findNextTarget(baseDay, targetDays);
        return current.withMonth(monthOfYear).withDayOfMonth(nextTarget);
    }
}
