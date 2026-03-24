package com.project.backend.domain.suggestion.invalidation.planner;

import com.project.backend.domain.suggestion.enums.SuggestionInvalidateReason;

/**
 * 단일 suggestion 무효화 명령
 */
public record InvalidationCommand(
        SuggestionInvalidateReason reason,
        byte[] targetHash
) {
}