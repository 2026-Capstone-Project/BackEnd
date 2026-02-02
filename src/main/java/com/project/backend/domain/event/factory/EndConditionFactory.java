package com.project.backend.domain.event.factory;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.strategy.endcondition.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 반복 패턴에 따라 알맞은 종료 전략을 주입하는 Factory
 */

@Component
@RequiredArgsConstructor
public class EndConditionFactory {

    private final DefaultEndCondition defaultEndCondition;
    private final NeverEndCondition neverEndCondition;
    private final EndByDateCondition endByDateCondition;
    private final EndByCountCondition endByCountCondition;

    public EndCondition getEndCondition(RecurrenceGroup rg) {

        // 단발성 이벤트
        if (rg == null) {
            return defaultEndCondition;
        }

        return switch (rg.getEndType()) {
            case NEVER -> neverEndCondition;
            case END_BY_DATE -> endByDateCondition;
            case END_BY_COUNT -> endByCountCondition;
        };
    }
}
