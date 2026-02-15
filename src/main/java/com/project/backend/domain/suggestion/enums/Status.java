package com.project.backend.domain.suggestion.enums;

/**
 * Status (제안 상태)
 *
 * Suggestion(AI 제안)의 처리 상태를 나타내는 Enum.
 * - PRIMARY: 첫 제안 (사용자가 아직 확인하지 않음)
 * - SECONDARY: 두 번째 제안 (사용자가 첫 제안에서 거절함)
 * - ACCEPTED: 수락됨 (해당 Category에 맞는 Event/Todo 생성됨)
 * - REJECTED: 거절됨 (사용자가 첫 제안을 거절하거나, 두 번쨰 제안을 거절 -> 일정 기간 뒤 삭제)
 * 상태 전이: PRIMARY -> SECONDARY -> ACCEPTED
 *                  \             \-> REJECTED
 *                   \-> ACCEPTED
 *                    \-> REJECTED
 */
public enum Status {
    PRIMARY,
    SECONDARY,
    ACCEPTED,
    REJECTED
}
