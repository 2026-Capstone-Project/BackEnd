package com.project.backend.domain.suggestion.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class SuggestionResDTO {

    public record LlmRes(
            long suggestionResCnt,
            List<LlmSuggestion> llmSuggestionList
    ) {
    }
    public record LlmSuggestion(
            Long eventId,
            String primaryContent,
            String secondaryContent
    ) {
    }
}
