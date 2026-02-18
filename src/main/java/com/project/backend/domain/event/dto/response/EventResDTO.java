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
            LocalDateTime occurrenceDate, // 항상 원본 발생일
            boolean calculated,
            String title,
            String content,
            LocalDateTime start, // 실제 표시/실행 시작시간
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
