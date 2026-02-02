package com.project.backend.domain.event.enums;

public enum RecurrenceEndType {
    NEVER,          // 종료 없음 (기본 3개월)
    END_BY_DATE,    // 특정 날짜까지
    END_BY_COUNT    // N회 반복 후 종료
}
