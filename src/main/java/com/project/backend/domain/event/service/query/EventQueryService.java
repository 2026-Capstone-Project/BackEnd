package com.project.backend.domain.event.service.query;

import com.project.backend.domain.occurrence.dto.TodayOccurrenceResult;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.occurrence.dto.NextOccurrenceResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface EventQueryService {
    EventResDTO.DetailRes getEventDetail(Long eventId, LocalDateTime occurrenceDate, Long memberId);

    EventResDTO.EventsListRes getEvents(Long memberId, LocalDate startDate, LocalDate endDate);

    NextOccurrenceResult calculateNextOccurrence(Long eventId, LocalDateTime occurrenceTime);

    LocalDateTime findNextOccurrenceAfterNow(Long eventId);

    List<TodayOccurrenceResult> calculateTodayOccurrence(List<Long> eventId, LocalDate currentDate);

    EventResDTO.EventTitleHistoryRes getEventTitleHistory(Long memberId, String keyword);

    EventResDTO.EventLocationHistoryRes getEventLocationHistory(Long memberId, String keyword);
}
