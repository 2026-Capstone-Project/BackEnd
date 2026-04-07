package com.project.backend.domain.event.service.command;

public interface EventParticipantCommandService {

    void acceptInvitation(Long memberId, Long eventParticipantId);

    void rejectInvitation(Long memberId, Long eventParticipantId);

}
