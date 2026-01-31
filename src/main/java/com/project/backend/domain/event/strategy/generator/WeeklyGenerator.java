package com.project.backend.domain.event.strategy.generator;

import com.project.backend.global.recurrence.RecurrenceRule;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WeeklyGenerator implements Generator {

    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceRule rule) {

        // 반복 요일
        String daysOfWeek = rule.getDaysOfWeek();

        // String으로 저장된 반복 요일을 DayOfWeek 가변 리스트로 변형
        List<DayOfWeek> days = Arrays.stream(daysOfWeek.split(","))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));

        // 다음 날짜 리턴
        return nearestDate(current, days);
    }

    // 매주 반복 요일에 따라서 입력한 날짜로부터 가장 가까운 날짜를 리턴
    private LocalDateTime nearestDate(LocalDateTime baseDate, List<DayOfWeek> targetDays) {

        // 입력된 날짜의 요일을 알아낸다
        DayOfWeek baseDayOfWeek = baseDate.getDayOfWeek();

        // 기준 요일에서 다음 기준으로 가까운 대상 요일 가져오기
        DayOfWeek rotatedTargetDays = findNextTarget(baseDayOfWeek, targetDays);

        // 기준 날보다 큰 반복 대상 중에서 가장 작은 날에 대해서
        int diff = (rotatedTargetDays.getValue() - baseDayOfWeek.getValue() + 7) % 7;

        if (diff == 0) {
            diff = 7;
        }

        return baseDate.plusDays(diff);
    }

    // TODO : 반복 리팩토링
    // 반복 대상에서 기준 다음이면서 가장 작은 값을 찾는 메서드
    private DayOfWeek findNextTarget(DayOfWeek baseDay, List<DayOfWeek> targetDays) {

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
