package com.project.backend.domain.event.service.query;

import com.project.backend.domain.event.converter.EventConverter;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EventQueryServiceImpl implements EventQueryService {

    private final EventRepository eventRepository;

    @Override
    public EventResDTO.DetailRes getEventDetail(Long eventId, Long memberId) {
        Event event = eventRepository.findByMemberIdAndId(memberId, eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        return EventConverter.toDetailRes(event);
    }
}
