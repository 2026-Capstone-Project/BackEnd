package com.project.backend.domain.event.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdjustedTime(
        LocalDateTime start,
        LocalDateTime end
) {}

