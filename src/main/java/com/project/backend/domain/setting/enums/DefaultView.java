package com.project.backend.domain.setting.enums;

/**
 * DefaultView (캘린더 기본 뷰)
 *
 * 캘린더 앱 접속 시 기본으로 보여줄 뷰를 설정하는 Enum.
 * - MONTH: 월간 뷰 (한 달 전체 일정 표시)
 * - WEEK: 주간 뷰 (한 주 일정 상세 표시)
 * - DAY: 일간 뷰 (하루 일정 타임라인 표시)
 *
 * 프론트엔드 캘린더 라이브러리(FullCalendar 등)의 initialView와 매핑.
 */
public enum DefaultView {
    MONTH,
    WEEK,
    DAY
}
