package com.project.backend.domain.event.validator;

import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.friend.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventParticipantValidator {

    private final FriendRepository friendRepository;

    public void validate(Long memberId, List<Long> participantIds) {
        // 공유할 사람이 없는 경우
        if (participantIds == null || participantIds.isEmpty()) {
            return;
        }

        // 자신의 memberId를 입력한 경우
        if (participantIds.contains(memberId)) {
            throw new EventException(EventErrorCode.EVENT_SELF_INVITE_NOT_ALLOWED);
        }

        // 리스트 내 중복 Id 거르기
        List<Long> distinctParticipantIds = participantIds.stream()
                .distinct()
                .toList();

        long friendCount = friendRepository.countByMemberIdAndOpponentIdIn(memberId, distinctParticipantIds);

        if (friendCount != distinctParticipantIds.size()) {
            throw new EventException(EventErrorCode.EVENT_INVITEE_NOT_FRIEND);
        }
    }
}
