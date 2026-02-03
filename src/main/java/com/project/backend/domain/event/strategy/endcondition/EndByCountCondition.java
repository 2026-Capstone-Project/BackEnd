package com.project.backend.domain.event.strategy.endcondition;

import com.project.backend.global.recurrence.RecurrenceRule;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EndByCountCondition implements EndCondition {

    // 생성 가능한 수를 넘으면 종료
    @Override
    public boolean shouldContinue(LocalDateTime time, int createdCount, RecurrenceRule rule) {
        return createdCount < rule.getOccurrenceCount();
    }
}
