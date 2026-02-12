package com.project.backend.domain.event.strategy.generator;

import com.project.backend.global.recurrence.RecurrenceRule;
import com.project.backend.global.recurrence.util.RecurrenceUtils;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class WeeklyGenerator implements Generator {

    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceRule rule) {

        // 반복 요일
        List<DayOfWeek> targetDays = RecurrenceUtils.parseDaysOfWeek(rule.getDaysOfWeek());

        // 반복 요일이 비어있으면 시작일의 요일을 기본값으로 사용
        if (targetDays.isEmpty()) {
            targetDays = List.of(current.getDayOfWeek());
        }

        // 다음 날짜 리턴
        return nearestDate(current, targetDays);
    }

    // 매주 반복 요일에 따라서 입력한 날짜로부터 가장 가까운 날짜를 리턴
    private LocalDateTime nearestDate(LocalDateTime baseDate, List<DayOfWeek> targetDays) {

        // 입력된 날짜의 요일을 알아낸다
        DayOfWeek baseDayOfWeek = baseDate.getDayOfWeek();

        // 기준 요일에서 다음 기준으로 가까운 대상 요일 가져오기
        DayOfWeek nextTargetDay = RecurrenceUtils.findNextTarget(baseDayOfWeek, targetDays);

        // 기준 날보다 큰 반복 대상 중에서 가장 작은 날에 대해서
        int diff = (nextTargetDay.getValue() - baseDayOfWeek.getValue() + 7) % 7;

        if (diff == 0) {
            diff = 7;
        }

        return baseDate.plusDays(diff);
    }
}
