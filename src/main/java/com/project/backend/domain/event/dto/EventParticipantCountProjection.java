package com.project.backend.domain.event.dto;

public interface EventParticipantCountProjection {
    Long getEventId();
    Long getParticipantCount();
}