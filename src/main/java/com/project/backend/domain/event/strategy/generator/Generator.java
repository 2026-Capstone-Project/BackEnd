package com.project.backend.domain.event.strategy.generator;

import com.project.backend.global.recurrence.RecurrenceRule;

import java.time.LocalDateTime;

/**
 *  반복 패턴에 따른 생성 전략
 *  각각의 Generator는 이 인터페이스를 구현합니다
 */
public interface Generator {

    /**
     *  패턴을 기준으로 current 다음에 올 가장 빠른 다음 time을 반환
     *  @param current 기준 시간
     *  @param rule 반복 규칙
     *  @return 다음 반복 시작 시간
     */
    LocalDateTime next(LocalDateTime current, RecurrenceRule rule);
}
