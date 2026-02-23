package com.project.backend.domain.suggestion.vo;

import com.project.backend.domain.suggestion.enums.SuggestionInvalidateReason;

public record SuggestionInvalidateEvent(
        Long memberId,
        byte[] targetKeyHash,
        SuggestionInvalidateReason reason
) {
}
