package com.project.backend.domain.event.strategy.generator.monthlyrule;

import com.project.backend.global.recurrence.RecurrenceRule;
import com.project.backend.global.recurrence.util.RecurrenceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class DayOfWeekRule implements MonthlyRule {

    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceRule rule) {

        int interval = rule.getIntervalValue();
        int weekOfMonth = rule.getWeekOfMonth();
        List<DayOfWeek> targetDays =
                RecurrenceUtils.parseDaysOfWeek(rule.getDayOfWeekInMonth());

        // 기준은 current가 아니라 "기준 월(anchor)"
        YearMonth baseMonth = YearMonth.from(current);

        // 다음 occurrence index
        int step = 1;

        while (true) {
            // interval만큼 월 점프
            YearMonth targetMonth = baseMonth.plusMonths((long) interval * step);

            Optional<LocalDate> date =
                    RecurrenceUtils.calculateMonthlyNthOrdinalWeekday(targetMonth, weekOfMonth, targetDays);

            if (date.isPresent()) {
                return LocalDateTime.of(date.get(), current.toLocalTime());
            }

            step++;
        }

        /*// N 번째 주 판별
        WeekFields wf = WeekFields.ISO;

        // 반복 주
        int weekOfMonth = rule.getWeekOfMonth();
        // 반복 요일
        String dayOfWeekInMonth = rule.getDayOfWeekInMonth();

        int interval = rule.getIntervalValue();

        int baseMonth = current.getMonth().getValue();

        // String으로 저장된 반복 요일을 DayOfWeek 리스트로
        List<DayOfWeek> targetDays = RecurrenceUtils.parseDaysOfWeek(dayOfWeekInMonth);

        // find next
        do {
            // 입력된 날짜의 요일을 알아낸다
            DayOfWeek baseDayOfWeek = current.getDayOfWeek();

            // 기준 요일에서 다음 기준으로 가까운 대상 요일 가져오기
            DayOfWeek targetDay = RecurrenceUtils.findNextTarget(baseDayOfWeek, targetDays);

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

        return current;*/
    }
}
