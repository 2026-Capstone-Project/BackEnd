package com.project.backend.domain.event.strategy.endcondition;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class EndByDateCondition implements EndCondition {

    // 생성 가능한 시점을 넘으면 종료
    public boolean shouldContinue(LocalDateTime time, int count, RecurrenceGroup rg) {
        return !time.isAfter(rg.getEndDate().atTime(LocalTime.MAX));
    }
}
