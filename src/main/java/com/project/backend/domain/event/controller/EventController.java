package com.project.backend.domain.event.controller;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.service.command.EventCommandService;
import com.project.backend.domain.event.service.query.EventQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController implements EventDocs {

    private final EventCommandService eventCommandService;
    private final EventQueryService eventQueryService;

    @PostMapping("")
    @Override
    public CustomResponse<EventResDTO.CreateRes> createEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody EventReqDTO.CreateReq createReq){
        EventResDTO.CreateRes resDTO = eventCommandService.createEvent(createReq, customUserDetails.getId());
        return CustomResponse.onSuccess("OK", resDTO);
    }

    @GetMapping("/{eventId}")
    @Override
    public CustomResponse<EventResDTO.DetailRes> getEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long eventId
    ){
        EventResDTO.DetailRes resDTO = eventQueryService.getEventDetail(eventId, customUserDetails.getId());
        return CustomResponse.onSuccess("OK", resDTO);
    }

    @PatchMapping("/{eventId}")
    public CustomResponse<EventResDTO.DetailRes> updateEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long eventId,
            @RequestBody EventReqDTO.UpdateReq req
    ){
        eventCommandService.updateEvent(req, eventId, customUserDetails.getId());
        return CustomResponse.onSuccess("수정 완료", null);
    }

}
