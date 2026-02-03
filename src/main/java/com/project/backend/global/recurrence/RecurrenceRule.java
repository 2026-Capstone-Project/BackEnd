package com.project.backend.global.recurrence;

import com.project.backend.domain.event.enums.MonthlyType;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;

import java.time.LocalDate;

/**
 * 반복 규칙 인터페이스
 * Event와 Todo가 공용으로 사용하는 반복 로직을 위한 추상화
 */
public interface RecurrenceRule {

    /**
     * 반복 주기 (DAILY, WEEKLY, MONTHLY, YEARLY)
     */
    RecurrenceFrequency getFrequency();

    /**
     * 반복 간격 (예: 2주마다 -> intervalValue = 2)
     */
    Integer getIntervalValue();

    /**
     * WEEKLY 반복: 반복 요일 (예: "MONDAY,WEDNESDAY,FRIDAY")
     */
    String getDaysOfWeek();

    /**
     * MONTHLY 반복: 반복 타입 (DAY_OF_MONTH 또는 DAY_OF_WEEK)
     */
    MonthlyType getMonthlyType();

    /**
     * MONTHLY 반복 (DAY_OF_MONTH): 반복 일자 (예: "1,15")
     */
    String getDaysOfMonth();

    /**
     * MONTHLY 반복 (DAY_OF_WEEK): N번째 주
     */
    Integer getWeekOfMonth();

    /**
     * MONTHLY 반복 (DAY_OF_WEEK): 반복 요일 (예: "MONDAY")
     */
    String getDayOfWeekInMonth();

    /**
     * YEARLY 반복: 반복 월 (1~12)
     */
    Integer getMonthOfYear();

    /**
     * 반복 종료 타입 (NEVER, END_BY_DATE, END_BY_COUNT)
     */
    RecurrenceEndType getEndType();

    /**
     * END_BY_DATE: 종료 날짜
     */
    LocalDate getEndDate();

    /**
     * END_BY_COUNT: 반복 횟수
     */
    Integer getOccurrenceCount();
}
