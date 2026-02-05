package com.project.backend.domain.suggestion.dto.request;

import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.StableType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class SuggestionReqDTO {

    @Builder
    public record LlmSuggestionDetail(
            Long eventId,
            String title,
            LocalDateTime start,
            Integer primaryDiff,
            Integer secondaryDiff,
            StableType stableType,
            Category category
    ) {
    }

    @Builder
    public record LlmSuggestionReq(
            long suggestionReqCnt,
            List<LlmSuggestionDetail> suggestionDetailList
    ) {
    }
}
