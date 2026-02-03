package com.project.backend.domain.event.strategy.endcondition;

import com.project.backend.global.recurrence.RecurrenceRule;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class EndByDateCondition implements EndCondition {

    // 생성 가능한 시점을 넘으면 종료
    @Override
    public boolean shouldContinue(LocalDateTime time, int count, RecurrenceRule rule) {
        return !time.isAfter(rule.getEndDate().atTime(LocalTime.MAX));
    }
}
