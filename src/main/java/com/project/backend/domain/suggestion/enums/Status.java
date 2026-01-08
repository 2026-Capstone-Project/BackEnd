package com.project.backend.domain.suggestion.enums;

/**
 * Status (제안 상태)
 *
 * Suggestion(AI 제안)의 처리 상태를 나타내는 Enum.
 * - PENDING: 대기 중 (사용자가 아직 확인하지 않음)
 * - ACCEPTED: 수락됨 (해당 Category에 맞는 Event/Todo 생성됨)
 * - REJECTED: 거절됨 (사용자가 필요 없다고 판단)
 *
 * 상태 전이: PENDING → ACCEPTED 또는 PENDING → REJECTED (단방향)
 */
public enum Status {
    PENDING,
    ACCEPTED,
    REJECTED
}
