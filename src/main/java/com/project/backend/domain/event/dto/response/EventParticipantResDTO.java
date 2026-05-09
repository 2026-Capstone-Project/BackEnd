package com.project.backend.domain.event.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class EventParticipantResDTO {

    @Builder
    public record InvitationRes(
            List<InvitationItem> invitations
    ){}

    @Builder
    public record InvitationItem(
            Long participantId,
            String ownerName,
            String title,
            LocalDateTime createdAt,
            LocalDate startDate,
            LocalDate endDate,
            String location,
            Long participantCount
    ){}

    @Builder
    public record SharedEventsRes(
            List<SharedEventItem> sharedEvents
    ) {}

    @Builder
    public record SharedEventItem(
            Long eventId,
            String ownerName,
            String title,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
