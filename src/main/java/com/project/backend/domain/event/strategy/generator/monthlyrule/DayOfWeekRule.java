package com.project.backend.domain.event.strategy.generator.monthlyrule;

import com.project.backend.global.recurrence.RecurrenceRule;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DayOfWeekRule implements MonthlyRule {

    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceRule rule) {

        // N 번째 주 판별
        WeekFields wf = WeekFields.ISO;

        // 반복 주
        int weekOfMonth = rule.getWeekOfMonth();
        // 반복 요일
        String dayOfWeekInMonth = rule.getDayOfWeekInMonth();

        int interval = rule.getIntervalValue();

        int baseMonth = current.getMonth().getValue();

        // String으로 저장된 반복 요일을 DayOfWeek 가변 리스트로
        List<DayOfWeek> targetDays = Arrays.stream(dayOfWeekInMonth.split(","))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));


        // find next
        do {
            // 입력된 날짜의 요일을 알아낸다
            DayOfWeek baseDayOfWeek = current.getDayOfWeek();

            // 기준 요일에서 다음 기준으로 가까운 대상 요일 가져오기
            DayOfWeek targetDay = findNextTarget(baseDayOfWeek, targetDays);

            // 기준 날보다 큰 반복 대상 중에서 가장 작은 날에 대해서
            int diff = (targetDay.getValue() - baseDayOfWeek.getValue() + 7) % 7;
            // 같은 요일이라면 다음주로
            if (diff == 0) {
                diff = 7;
            }
            current = current.plusDays(diff);

            // TODO : 끔찍한 코드 리팩토링하기
            // 만약 달이 바뀌었다면
            if (baseMonth != current.getMonth().getValue()) {
                // 기준 시간을 M달 뒤 첫 번째 날로 설정
                LocalDateTime tempCurrent = current.plusMonths(interval - 1).withDayOfMonth(1);
                // 만약 내가 찾던 N 번째 주라면
                if (tempCurrent.get(wf.weekOfMonth()) == weekOfMonth) {
                    // 만약 내가 찾던 요일이 포함되어 있는지
                    if (dayOfWeekInMonth.contains(tempCurrent.getDayOfWeek().toString())) {
                        return current;
                    }
                    // 아니면 기준 시간을 M달의 첫 번째 날로 설정
                    current = current.plusMonths(interval).withDayOfMonth(1);
                } else {
                    current = tempCurrent;
                }
                // 기준 달을 변경하여 달이 변경될 시점까지 if문 비활성화
                baseMonth = current.getMonth().getValue();
            }
        }
        // 현재 시간이 N 주차를 만족할 때까지
        while (current.get(wf.weekOfMonth()) != weekOfMonth);

        return current;
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