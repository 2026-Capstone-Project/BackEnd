package com.project.backend.domain.event.strategy.endcondition;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NeverEndCondition implements EndCondition {

    // 무한 반복
    @Override
    public boolean shouldContinue(LocalDateTime next, int count, RecurrenceGroup rg) {
        return true;
    }
}
