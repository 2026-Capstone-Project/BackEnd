package com.project.backend.domain.setting.enums;

/**
 * ReminderTiming (리마인더 타이밍)
 *
 * 일정 알림을 몇 분 전에 받을지 설정하는 Enum.
 * minutes 필드로 실제 분 단위 값을 저장하여 알림 스케줄링에 활용.
 *
 * - FIVE_MINUTES: 5분 전
 * - FIFTEEN_MINUTES: 15분 전
 * - THIRTY_MINUTES: 30분 전
 * - ONE_HOUR: 1시간 전 (기본값)
 * - TWO_HOURS: 2시간 전
 * - ONE_DAY: 1일 전 (1440분)
 *
 * 사용 예시:
 * LocalDateTime reminderTime = event.getStartTime().minusMinutes(setting.getReminderTiming().getMinutes());
 */
public enum ReminderTiming {
    FIVE_MINUTES(5),
    FIFTEEN_MINUTES(15),
    THIRTY_MINUTES(30),
    ONE_HOUR(60),
    TWO_HOURS(120),
    ONE_DAY(1440);

    private final int minutes;

    ReminderTiming(int minutes) {
        this.minutes = minutes;
    }

    public int getMinutes() {
        return minutes;
    }
}
