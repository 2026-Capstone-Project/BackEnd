package com.project.backend.domain.event.strategy.generator;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class YearlyGenerator implements Generator {

    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceGroup rg) {

        int monthOfYear = rg.getMonthOfYear();

        String daysOfMonth = rg.getDaysOfMonth();

        List<Integer> targetDays = Arrays.stream(daysOfMonth.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(ArrayList::new));

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
            current = current.plusYears(1);
        }

        // 다음 반복 대상
        int nextTarget = findNextTarget(baseDay, targetDays);
        return current.withMonth(monthOfYear).withDayOfMonth(nextTarget);
    }

    // TODO : 반복 리팩토링
    // 반복 대상에서 기준 다음이면서 가장 작은 값을 찾는 메서드
    private int findNextTarget(int baseDay, List<Integer> targetDays) {

        // 명시적 정렬
        java.util.Collections.sort(targetDays);

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