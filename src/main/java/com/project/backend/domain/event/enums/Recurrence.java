package com.project.backend.domain.event.enums;

/**
 * Recurrence (반복 주기)
 *
 * Event(일정)의 반복 설정을 나타내는 Enum.
 * - NONE: 반복 없음 (1회성 일정)
 * - DAY: 매일 반복
 * - WEEK: 매주 반복 (같은 요일)
 * - MONTH: 매월 반복 (같은 날짜)
 *
 * 반복 일정 조회 시 원본 일정의 startTime 기준으로
 * 반복 인스턴스를 동적으로 생성하는 로직 필요.
 */
public enum Recurrence {
    NONE,
    DAY,
    WEEK,
    MONTH
}

