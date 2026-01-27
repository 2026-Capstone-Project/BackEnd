package com.project.backend.domain.event.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

public class EventResDTO {

    @Builder
    public record CreateRes(
            Long id,
            LocalDateTime createdAt
    ) {
    }
}
