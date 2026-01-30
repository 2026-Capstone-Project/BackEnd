package com.project.backend.domain.event.dto.response;

import com.project.backend.domain.event.enums.EventColor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class EventResDTO {

    @Builder
    public record CreateRes(
            Long id,
            LocalDateTime createdAt
    ) {
    }

    @Builder
    public record DetailRes(
            Long id,
            String title,
            String content,
            LocalDateTime start,
            LocalDateTime end,
            String location,
            Boolean isAllDay,
            EventColor color,
            RecurrenceGroupResDTO.DetailRes recurrenceGroup
    ) {
    }

    @Builder
    public record EventsListRes(
            List<DetailRes> details
    ) {
    }
}
