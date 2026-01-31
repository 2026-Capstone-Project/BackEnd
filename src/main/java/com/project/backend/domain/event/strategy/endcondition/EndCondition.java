package com.project.backend.domain.event.strategy.endcondition;

import com.project.backend.domain.event.entity.RecurrenceGroup;

import java.time.LocalDateTime;

/**
 *  반복 패턴에 따라 Generator의 동작 여부를 결정하는
 *  EndCondition의 인터페이스
 */
public interface EndCondition {

    /**
     * @param time 해당 임시 객체를 생성하기 위한 기준 시간
     * @param createdCount 임시 객체 생성 개수
     * @param rg 반복 그룹
     * @return Generator를 작동시켜도 되는지
     */
    boolean shouldContinue(LocalDateTime time, int createdCount, RecurrenceGroup rg);
}
