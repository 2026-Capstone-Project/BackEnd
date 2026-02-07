package com.project.backend.domain.suggestion.vo;

import com.project.backend.domain.event.dto.response.RecurrenceGroupResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.enums.EventColor;
import com.project.backend.domain.suggestion.enums.RecurrencePatternType;

import java.time.LocalDateTime;

public record SuggestionCandidate(
        Long id,
        String title,
        String location,
        String content,
        LocalDateTime start,
        LocalDateTime end,
        Integer durationMinutes,
        Boolean isAllDay,
        SuggestionPattern primary,
        SuggestionPattern secondary,
        RecurrencePatternType patternType,
        EventColor color,
        RecurrenceGroupResDTO.DetailRes recurrenceGroup
) {
    public static SuggestionCandidate from(Event event) {
        return new SuggestionCandidate(
                event.getId(),
                event.getLocation(),
                event.getTitle(),
                event.getContent(),
                event.getStartTime(),
                event.getEndTime(),
                event.getDurationMinutes(),
                event.getIsAllDay(),
                null,
                null,
                null,
                event.getColor(),
                null
        );
    }

    public SuggestionCandidate withPattern(SuggestionPattern primary, SuggestionPattern secondary, RecurrencePatternType patternType) {
        return new SuggestionCandidate(
                this.id,
                this.title,
                this.location,
                this.content,
                this.start,
                this.end,
                this.durationMinutes,
                this.isAllDay,
                primary,
                secondary,
                patternType,
                this.color,
                this.recurrenceGroup
        );
    }
}
