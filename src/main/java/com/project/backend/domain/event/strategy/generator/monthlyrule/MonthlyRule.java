package com.project.backend.domain.event.strategy.generator.monthlyrule;

import com.project.backend.global.recurrence.RecurrenceRule;

import java.time.LocalDateTime;

/**
 *  반복 패턴이 MONTHLY인 경우
 *  Rule 1. M 달마다 [N...] 일
 *  Rule 2. M 달마다 N 번째 주의 [X...] 요일
 *  각각의 Rule은 이 인터페이스를 구현합니다
 */
public interface MonthlyRule {

    /**
     *  패턴을 기준으로 current 다음에 올 가장 빠른 다음 time을 반환
     *  @param current 기준 시간
     *  @param rule 반복 규칙
     *  @return 다음 반복 시작 시간
     */
    LocalDateTime next(LocalDateTime current, RecurrenceRule rule);
}
