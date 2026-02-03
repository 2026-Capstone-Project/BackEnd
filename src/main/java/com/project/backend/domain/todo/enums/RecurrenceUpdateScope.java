package com.project.backend.domain.todo.enums;

/**
 * 반복 할 일 수정/삭제 범위
 */
public enum RecurrenceUpdateScope {
    THIS_TODO,           // 이 할 일만
    THIS_AND_FOLLOWING,  // 이 할 일 및 이후 모든 할 일
    ALL_TODOS            // 모든 할 일
}
