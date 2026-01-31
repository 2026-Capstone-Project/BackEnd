package com.project.backend.domain.event.strategy.endcondition;

import com.project.backend.global.recurrence.RecurrenceRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DefaultEndCondition implements EndCondition {

    // 단발성 이벤트는 바로 종료
    @Override
    public boolean shouldContinue(LocalDateTime time, int createdCount, RecurrenceRule rule) {
        log.info("It's defaultEndCondition");
        return false;
    }
}
