package com.project.backend.domain.reminder.enums;

public enum LifecycleStatus {
    ACTIVE, // 활성화
    INACTIVE, // 비활성화 (다음 일정/할일 이 있지만 현재 시간보다 이전 일정/할일의 시간이라 리마인더 재료로 안쓰일 때)
    TERMINATED // 삭제 예정
}
