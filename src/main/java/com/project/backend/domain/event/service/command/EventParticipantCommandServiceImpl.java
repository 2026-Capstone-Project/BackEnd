package com.project.backend.domain.event.service.command;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.EventParticipant;
import com.project.backend.domain.event.enums.InviteStatus;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.repository.EventParticipantRepository;
import com.project.backend.domain.event.repository.EventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class EventParticipantCommandServiceImpl implements EventParticipantCommandService{

    private final EventParticipantRepository eventParticipantRepository;
    private final EventRepository eventRepository;

    @Override
    public void acceptInvitation(Long memberId, Long eventParticipantId) {
        EventParticipant eventParticipant = getEventParticipant(memberId, eventParticipantId);

        eventParticipant.accept();
    }

    @Override
    public void rejectInvitation(Long memberId, Long eventParticipantId) {
        // 이벤트 공유 초대 객체 조회 (status = PENDING)
        EventParticipant eventParticipant = getEventParticipant(memberId, eventParticipantId);

        eventParticipantRepository.delete(eventParticipant);
    }

    @Override
    public void leaveSharedEvent(Long memberId, Long eventId) {
        // 이벤트 객체 조회
        Event ownerEvent  = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));
        // 이벤트의 소유주가 탈퇴하려고 할 때
        if (ownerEvent.getMember().getId().equals(memberId)) {
            throw new EventException(EventErrorCode.EVENT_OWNER_CANNOT_LEAVE);
        }
        // 멤버 아이디와 이벤트 아이디로 객체 조회
        EventParticipant eventParticipant =
                eventParticipantRepository.findByMemberIdAndEventId(memberId, eventId)
                        .orElseThrow(() -> new EventException(EventErrorCode.EVENT_INVITATION_NOT_FOUND));

        // 공유 탈퇴시 바로 연관 관계 삭제
        eventParticipantRepository.delete(eventParticipant);
    }

    // EventParticipant 객체 검증 후 반환
    private EventParticipant getEventParticipant(Long memberId, Long eventParticipantId) {
        // 이벤트 공유 초대 객체 조회 (status = PENDING)
        EventParticipant eventParticipant =
                eventParticipantRepository.findByIdAndStatus(eventParticipantId, InviteStatus.PENDING)
                        .orElseThrow(() -> new EventException(EventErrorCode.EVENT_INVITATION_NOT_FOUND));
        // 초대 객체 소유권 확인
        if (!eventParticipant.getMember().getId().equals(memberId)) {
            throw new EventException(EventErrorCode.EVENT_INVITATION_FORBIDDEN);
        }
        return eventParticipant;
    }
}
