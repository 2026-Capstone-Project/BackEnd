package com.project.backend.domain.event.service.query;

import com.project.backend.domain.event.dto.response.EventResDTO;

public interface EventQueryService {
    EventResDTO.DetailRes getEventDetail(Long eventId, Long memberId);
}
