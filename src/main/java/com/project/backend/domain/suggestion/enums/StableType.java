package com.project.backend.domain.suggestion.enums;
/**
 * StableType (반복 패턴 안정 상태)
 * - PERFECTLY_STABLE: 가장 최근 2개의 반복 패턴이 연속인 경우
 * - [1, 2, 7, 7], [7, 8, ,7, 7]
 * - PARTIALLY_STABLE: 가장 최근의 반복 패턴을 제외하고 2개의 반복 패턴이 연속인 경우
 * - [1, 2, 2, 7], [7, 7, ,7, 14]
 * - CONTAMINATED_STABLE: PERFECTLY, PARTIALLY가 아닌 경우
 * - [7, 7, 7, 14, 7]
 */
public enum StableType {
    PERFECTLY_STABLE,
    PARTIALLY_STABLE,
    CONTAMINATED_STABLE,
}
