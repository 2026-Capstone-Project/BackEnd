package com.project.backend.domain.event.service.command;

public interface EventParticipantCommandService {

    void acceptInvitation(Long memberId, Long eventParticipantId);
}
