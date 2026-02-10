package com.project.backend.domain.event.controller;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
import com.project.backend.domain.event.service.command.EventCommandService;
import com.project.backend.domain.event.service.query.EventQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import java.time.LocalDate;

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
        return CustomResponse.onSuccess("이벤트 생성 완료", resDTO);
    }

    @GetMapping("/{eventId}")
    @Override
    public CustomResponse<EventResDTO.DetailRes> getEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long eventId,
            @RequestParam(required = false) LocalDateTime occurrenceDate
    ){
        EventResDTO.DetailRes resDTO = eventQueryService.getEventDetail(
                eventId,
                occurrenceDate,
                customUserDetails.getId());
        return CustomResponse.onSuccess("이벤트 단일 조회 완료", resDTO);
    }

    @GetMapping()
    @Override
    public CustomResponse<EventResDTO.EventsListRes> getEvents(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        EventResDTO.EventsListRes resDTO =
                eventQueryService.getEvents(customUserDetails.getId(), startDate, endDate);
        return CustomResponse.onSuccess("전체 이벤트 조회 완료", resDTO);
    }

    @PatchMapping("/{eventId}")
    @Override
    public CustomResponse<Void> updateEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long eventId,
            @RequestBody EventReqDTO.UpdateReq req
    ){
        eventCommandService.updateEvent(req, eventId, customUserDetails.getId());
        return CustomResponse.onSuccess("수정 완료", null);
    }

    @DeleteMapping("/{eventId}")
    @Override
    public CustomResponse<Void> deleteEvent(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long eventId,
            @RequestParam(required = false) LocalDate occurrenceDate,
            @RequestParam(required = false) RecurrenceUpdateScope scope
    ){
        eventCommandService.deleteEvent(eventId, occurrenceDate, scope, customUserDetails.getId());
        return CustomResponse.onSuccess("삭제 완료", null);
    }

}
