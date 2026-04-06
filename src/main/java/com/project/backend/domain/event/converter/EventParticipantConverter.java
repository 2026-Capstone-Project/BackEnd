package com.project.backend.domain.event.converter;

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

    public static EventParticipant toEventParticipant(Event event, Member member, Long ownerId) {
        return EventParticipant.builder()
                .ownerId(ownerId)
                .status(InviteStatus.PENDING)
                .event(event)
                .member(member)
                .build();
    }
}
