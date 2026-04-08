package com.project.backend.domain.event.controller;

import com.project.backend.domain.event.dto.response.EventParticipantResDTO;
import com.project.backend.domain.event.service.command.EventCommandService;
import com.project.backend.domain.event.service.command.EventParticipantCommandService;
import com.project.backend.domain.event.service.query.EventParticipantQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.csrf.repository.CustomCookieCsrfTokenRepository;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/event-participant")
@RequiredArgsConstructor
public class EventParticipantController implements EventParticipantDocs{

    public final EventParticipantQueryService eventParticipantQueryService;
    private final EventParticipantCommandService eventParticipantCommandService;

    @Override
    @GetMapping("/shared-events")
    public CustomResponse<EventParticipantResDTO.SharedEventsRes> getSharedEvents(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        EventParticipantResDTO.SharedEventsRes resDto =
                eventParticipantQueryService.getSharedEvents(customUserDetails.getId());
        return CustomResponse.onSuccess("공유 중인 일정 목록 조회 완료", resDto);
    }

    @Override
    @GetMapping("/invitations")
    public CustomResponse<EventParticipantResDTO.InvitationRes> getEventInvitations(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        EventParticipantResDTO.InvitationRes resDto =
                eventParticipantQueryService.getInvitations(customUserDetails.getId());
        return CustomResponse.onSuccess("일정 참여 초대 목록 조회 완료", resDto);
    }

    @Override
    @PostMapping("/{eventParticipantId}/acceptance")
    public CustomResponse<String> acceptInvitation(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long eventParticipantId
    ) {
        eventParticipantCommandService.acceptInvitation(customUserDetails.getId(), eventParticipantId);
        return CustomResponse.onSuccess("일정 참여 초대 수락 완료", null);
    }

    @Override
    @PostMapping("/{eventParticipantId}/rejection")
    public CustomResponse<String> rejectInvitation(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long eventParticipantId
    ) {
        eventParticipantCommandService.rejectInvitation(customUserDetails.getId(), eventParticipantId);
        return CustomResponse.onSuccess("일정 참여 초대 거절 완료", null);
    }
}
