package com.project.backend.domain.suggestion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * StableType (반복 패턴 안정 상태)
 * - PERFECTLY_STABLE: 가장 최근 2개의 반복 패턴이 연속인 경우
 * - [1, 2, 7, 7], [7, 8, ,7, 7]
 * - PARTIALLY_STABLE: 가장 최근의 반복 패턴을 제외하고 2개의 반복 패턴이 연속인 경우
 * - [1, 2, 2, 7], [7, 7, ,7, 14]
 * - CONTAMINATED_STABLE: PERFECTLY, PARTIALLY가 아닌 경우인데, 패턴이 약간 있는 경우
 * - [7, 7, 7, 14, 7]
 */

@Getter
@RequiredArgsConstructor
public enum StableType {
    PERFECTLY_STABLE(3),
    PARTIALLY_STABLE(2),
    CONTAMINATED_STABLE(1),
    ;

    private final int score;
}
