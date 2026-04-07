package com.project.backend.domain.event.service.query;

import com.project.backend.domain.event.converter.EventParticipantConverter;
import com.project.backend.domain.event.dto.EventParticipantCountProjection;
import com.project.backend.domain.event.dto.response.EventParticipantResDTO;
import com.project.backend.domain.event.entity.EventParticipant;
import com.project.backend.domain.event.repository.EventParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventParticipantQueryServiceImpl implements EventParticipantQueryService{

    private final EventParticipantRepository eventParticipantRepository;

    @Override
    public EventParticipantResDTO.InvitationRes getInvitations(Long memberId) {

        List<EventParticipant> participantList
                = eventParticipantRepository.findAllByMemberId(memberId);

        List<Long> eventIds = participantList.stream()
                .map(participant -> participant.getEvent().getId())
                .distinct()
                .toList();

        Map<Long, Long> participantCountMap = eventIds.isEmpty()
                ? Collections.emptyMap()
                : eventParticipantRepository.countParticipantsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        EventParticipantCountProjection::getEventId,
                        EventParticipantCountProjection::getParticipantCount
                ));

        List<EventParticipantResDTO.InvitationItem> items = participantList.stream()
                .map(participant ->
                        EventParticipantConverter.toInvitationItem(
                                participant,
                                participantCountMap.getOrDefault(participant.getEvent().getId(), 0L)
                        )
                )
                .toList();

        return EventParticipantConverter.toInvitationRes(items);
    }
}
