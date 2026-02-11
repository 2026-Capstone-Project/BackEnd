package com.project.backend.domain.suggestion.dto.response;

import lombok.Builder;

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

    public record LlmRecurrenceGroupSuggestionRes(
            long suggestionResCnt,
            List<LlmRecurrenceGroupSuggestion> llmRecurrenceGroupSuggestionList
    ) {
    }

    public record LlmRecurrenceGroupSuggestion(
            Long recurrenceGroupId,
            String content
    ) {
    }

    @Builder
    public record SuggestionListRes(
            List<SuggestionDetailRes> details
    ) {
    }

    @Builder
    public record SuggestionDetailRes(
            Long id,
            String content
    ) {
    }
}
