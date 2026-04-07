package com.project.backend.domain.event.service;

import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.friend.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventParticipantResolver {

    private final FriendRepository friendRepository;


    public List<Long> resolveParticipantIds(Long memberId, List<Long> friendIds) {
        // 공유할 사람이 없는 경우
        if (friendIds == null) {
            return Collections.emptyList();
        }

        // 리스트 내 중복 Id 거르기
        List<Long> distinctFriendIds = friendIds.stream()
                .distinct()
                .toList();

        log.info("distinctFriendIds = {}", distinctFriendIds);
        // 입력한 친구 아이디를 바탕으로 해당 친구 memberId를 가져옴
        List<Long> participantIds =
                friendRepository.findOpponentMemberIdsByFriendIdsAndMemberId(distinctFriendIds, memberId);

        log.info("participantIds = {}", participantIds);
        // 자신의 memberId를 입력한 경우
        if (participantIds.contains(memberId)) {
            throw new EventException(EventErrorCode.EVENT_SELF_INVITE_NOT_ALLOWED);
        }

        // participants 속 맴버 아이디들의 주인이 유저와 실제 친구인지 확인
        if (participantIds.size() != distinctFriendIds.size()) {
            throw new EventException(EventErrorCode.EVENT_INVITEE_NOT_FRIEND);
        }

        return participantIds;
    }
}
