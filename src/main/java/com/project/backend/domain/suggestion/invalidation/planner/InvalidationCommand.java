package com.project.backend.domain.suggestion.invalidation.planner;

import com.project.backend.domain.suggestion.enums.SuggestionInvalidateReason;

public record InvalidationCommand(
        SuggestionInvalidateReason reason,
        byte[] targetHash
) {
}
