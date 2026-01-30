package com.project.backend.domain.event.service.command;


import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;

import java.time.LocalDate;

public interface EventCommandService {

    EventResDTO.CreateRes createEvent(EventReqDTO.CreateReq req, Long memberId);

    void updateEvent(EventReqDTO.UpdateReq req, Long eventId, Long memberId);

    void deleteEvent(Long eventId, LocalDate occurrenceDate, RecurrenceUpdateScope scope, Long memberId);
}
