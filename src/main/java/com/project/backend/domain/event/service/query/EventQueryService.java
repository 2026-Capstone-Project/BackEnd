package com.project.backend.domain.event.service.query;

import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.reminder.dto.NextOccurrenceResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface EventQueryService {
    EventResDTO.DetailRes getEventDetail(Long eventId, LocalDateTime time, Long memberId);

    EventResDTO.EventsListRes getEvents(Long memberId, LocalDate startDate, LocalDate endDate);

    NextOccurrenceResult calculateNextOccurrence(Long eventId, LocalDateTime occurrenceTime);

    LocalDateTime findNextOccurrenceAfterNow(Long eventId);
}
