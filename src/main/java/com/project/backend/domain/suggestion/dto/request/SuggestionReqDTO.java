package com.project.backend.domain.suggestion.dto.request;

import com.project.backend.domain.suggestion.vo.SuggestionPattern;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.RecurrencePatternType;
import com.project.backend.domain.suggestion.enums.StableType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class SuggestionReqDTO {

    @Builder
    public record LlmSuggestionDetail(
            Long eventId,
            String title,
            LocalDateTime start,
            Category category,

            RecurrencePatternType patternType,
            StableType stableType,

            SuggestionPattern primaryPattern,
            SuggestionPattern secondaryPattern
    ) {
    }

    @Builder
    public record LlmSuggestionReq(
            long suggestionReqCnt,
            List<LlmSuggestionDetail> suggestionDetailList
    ) {
    }

    @Builder
    public record LlmRecurrenceGroupSuggestionReq(
            long suggestionReqCnt,
            List<LlmRecurrenceGroupSuggestionDetail> recurrenceGroupSuggestionDetails
    ) {
    }

    @Builder
    public record LlmRecurrenceGroupSuggestionDetail(
            Long recurrenceGroupId,
            String title,
            LocalDateTime lastStartTime,
            LocalDate endDate,
            Integer occurrenceCount
    ) {
    }
}
