package com.project.backend.domain.event.service.command;

import com.project.backend.domain.event.entity.EventParticipant;
import com.project.backend.domain.event.enums.InviteStatus;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.repository.EventParticipantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class EventParticipantCommandServiceImpl implements EventParticipantCommandService{

    private final EventParticipantRepository eventParticipantRepository;

    @Override
    public void acceptInvitation(Long memberId, Long eventParticipantId) {
        // 이벤트 공유 초대 객체 조회 (status = PENDING)
        EventParticipant eventParticipant =
                eventParticipantRepository.findByIdAndStatus(eventParticipantId, InviteStatus.PENDING)
                        .orElseThrow(() -> new EventException(EventErrorCode.EVENT_INVITATION_NOT_FOUND));
        // 초대 객체 소유권 확인
        if (!eventParticipant.getId().equals(memberId)) {
            throw new EventException(EventErrorCode.EVENT_INVITATION_FORBIDDEN);
        }

        eventParticipant.accept();
    }
}
