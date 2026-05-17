package com.project.backend.domain.event.dto;

import com.project.backend.domain.event.entity.EventParticipant;

import java.util.List;
import java.util.Map;

public record EventParticipantViewContext(
        List<EventParticipant> participants,
        boolean isOwner,
        boolean isAcceptedParticipant,
        Map<Long, Long> friendIdByMemberId
) {
}
