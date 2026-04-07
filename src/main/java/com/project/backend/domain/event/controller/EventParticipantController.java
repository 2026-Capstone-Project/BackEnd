package com.project.backend.domain.event.controller;

import com.project.backend.domain.event.dto.response.EventParticipantResDTO;
import com.project.backend.domain.event.service.query.EventParticipantQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/event-participant")
@RequiredArgsConstructor
public class EventParticipantController {

    public final EventParticipantQueryService eventParticipantQueryService;

    @GetMapping("/invitations")
    public CustomResponse<EventParticipantResDTO.InvitationRes> getEventInvitations(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        EventParticipantResDTO.InvitationRes resDto =
                eventParticipantQueryService.getInvitations(customUserDetails.getId());
        return CustomResponse.onSuccess("일정 참여 초대 목록 조회 완료", resDto);
    }
}
