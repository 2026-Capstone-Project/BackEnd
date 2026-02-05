package com.project.backend.domain.suggestion.vo;

import com.project.backend.domain.event.dto.response.RecurrenceGroupResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.enums.EventColor;

import java.time.LocalDateTime;

public record SuggestionCandidate(
        Long id,
        Integer primaryDiff,
        Integer secondaryDiff,
        String title,
        String content,
        LocalDateTime start,
        LocalDateTime end,
        Integer durationMinutes,
        String location,
        Boolean isAllDay,
        EventColor color,
        RecurrenceGroupResDTO.DetailRes recurrenceGroup
) {
    public static SuggestionCandidate from(Event event) {
        return new SuggestionCandidate(
                event.getId(),
                null,
                null,
                event.getTitle(),
                event.getContent(),
                event.getStartTime(),
                event.getEndTime(),
                event.getDurationMinutes(),
                event.getLocation(),
                event.getIsAllDay(),
                event.getColor(),
                null
        );
    }

    public SuggestionCandidate withDiff(Integer primaryDiff, Integer secondaryDiff) {
        return new SuggestionCandidate(
                this.id,
                primaryDiff,
                secondaryDiff,
                this.title,
                this.content,
                this.start,
                this.end,
                this.durationMinutes,
                this.location,
                this.isAllDay,
                this.color,
                this.recurrenceGroup
        );
    }
}
