package com.project.backend.domain.event.converter;

import com.project.backend.domain.event.enums.EventColor;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventSpec(
        String title,
        String content,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        EventColor color,
        Boolean isAllDay
) {}