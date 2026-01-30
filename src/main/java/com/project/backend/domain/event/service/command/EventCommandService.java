package com.project.backend.domain.event.service.command;


import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;

public interface EventCommandService {

    EventResDTO.CreateRes createEvent(EventReqDTO.CreateReq req, Long memberId);

    void updateEvent(EventReqDTO.UpdateReq req, Long eventId, Long memberId);
}
