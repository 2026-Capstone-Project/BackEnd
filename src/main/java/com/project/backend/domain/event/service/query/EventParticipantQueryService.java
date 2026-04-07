package com.project.backend.domain.event.service.query;

import com.project.backend.domain.event.dto.response.EventParticipantResDTO;

public interface EventParticipantQueryService {
    EventParticipantResDTO.InvitationRes getInvitations(Long memberId);
}
