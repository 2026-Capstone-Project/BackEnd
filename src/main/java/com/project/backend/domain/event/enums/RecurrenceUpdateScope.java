package com.project.backend.domain.event.enums;

public enum RecurrenceUpdateScope {
    THIS_EVENT,  // 이 일정
    THIS_AND_FOLLOWING_EVENTS, // 이 일정 및 향후 일정
    ALL_EVENTS // 이 일정을 포함한 모든 일정
}
