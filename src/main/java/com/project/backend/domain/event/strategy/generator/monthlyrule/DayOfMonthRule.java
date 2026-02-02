package com.project.backend.domain.event.strategy.generator.monthlyrule;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DayOfMonthRule implements MonthlyRule {

    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceGroup rg) {

        // M달
        int interval = rg.getIntervalValue();
        // X일
        String daysOfMonth = rg.getDaysOfMonth();

        // String으로 저장된 반복 일을 Integer 가변 리스트로
        List<Integer> targetDays = Arrays.stream(daysOfMonth.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(ArrayList::new));

        int baseDay = current.getDayOfMonth();

        while (true) {
            int nextTarget = findNextTarget(baseDay, targetDays);
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

    // TODO : 반복 리팩토링
    // 반복 대상에서 기준 다음이면서 가장 작은 값을 찾는 메서드
    private int findNextTarget(int baseDay, List<Integer> targetDays) {

        // 명시적 정렬
        Collections.sort(targetDays);

        // 바이너리 서치
        int idx = Collections.binarySearch(targetDays, baseDay);

        // 바이너리 서치에서 정확한 값을 찾지 못한 경우
        if (idx < 0) {
            // 삽입 위치의 다음 인덱스
            idx = -idx - 1;
            // NPE 방지
            idx = idx % targetDays.size();

            return targetDays.get(idx);
        }
        // 정확히 찾았다면 그 다음 값을 반환
        return targetDays.get((idx + 1) % targetDays.size());
    }
}