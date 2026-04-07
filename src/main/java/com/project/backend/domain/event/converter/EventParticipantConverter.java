package com.project.backend.domain.event.converter;

import com.project.backend.domain.event.dto.response.EventParticipantResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.EventParticipant;
import com.project.backend.domain.event.enums.InviteStatus;
import com.project.backend.domain.member.entity.Member;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class EventParticipantConverter {

    public static EventParticipant toEventParticipant(Event event, Member member, Member owner) {
        return EventParticipant.builder()
                .status(InviteStatus.PENDING)
                .event(event)
                .member(member)
                .owner(owner)
                .build();
    }

    public static EventParticipantResDTO.InvitationRes toInvitationRes(
            List<EventParticipantResDTO.InvitationItem> invitations
    ) {
        return EventParticipantResDTO.InvitationRes.builder()
                .invitations(invitations != null ? invitations : List.of())
                .build();
    }

    public static EventParticipantResDTO.InvitationItem toInvitationItem(
            EventParticipant eventParticipant,
            Long participantCount
    ) {
        Event event = eventParticipant.getEvent();

        return EventParticipantResDTO.InvitationItem.builder()
                .participantId(eventParticipant.getId())
                .ownerName(eventParticipant.getOwner().getNickname())
                .createdAt(eventParticipant.getCreatedAt())
                .title(event.getTitle())
                .startDate(event.getStartTime().toLocalDate())
                .endDate(event.getEndTime().toLocalDate())
                .location(event.getLocation())
                .participantCount(participantCount + 1) // 주최자 포함
                .build();
    }

}
