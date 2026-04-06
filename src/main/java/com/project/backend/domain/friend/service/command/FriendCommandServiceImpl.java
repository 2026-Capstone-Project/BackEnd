package com.project.backend.domain.friend.service.command;

import com.project.backend.domain.friend.converter.FriendConverter;
import com.project.backend.domain.friend.converter.FriendRequestConverter;
import com.project.backend.domain.friend.dto.request.FriendReqDTO;
import com.project.backend.domain.friend.entity.Friend;
import com.project.backend.domain.friend.entity.FriendRequest;
import com.project.backend.domain.friend.exception.FriendErrorCode;
import com.project.backend.domain.friend.exception.FriendException;
import com.project.backend.domain.friend.repository.FriendRepository;
import com.project.backend.domain.friend.repository.FriendRequestRepository;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.enums.Role;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FriendCommandServiceImpl implements FriendCommandService{

    private final MemberRepository memberRepository;
    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;

    @Override
    public void sendRequest(Long memberId, FriendReqDTO.SendRequestReq reqDTO) {

        // 요청자 객체
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // memberId : 요청자, reqDTO.email() : 피요청자
        // 피요청자 객체
        // 이미 친구이거나, 친구 요청을 보낸 상태라면 409 반환
        Member requestTarget = getRequestTarget(memberId, reqDTO.email());

        // 만약 피요청자가 요청자에게 친구 요청 조회
        FriendRequest reversedFriendRequest =
                friendRequestRepository.findBySenderIdAndReceiverId(requestTarget.getId(), memberId)
                        .orElse(null);

        // 만약 피요청자가 요청자에게 친구 요청을 보냈다면
        if (reversedFriendRequest != null) {
            saveFriend(reversedFriendRequest);
            return;
        }

        // 피요청자가 요청자에게 친구 요청을 보내지 않았다면
        // 요청 객체 생성
        FriendRequest friendRequest = FriendRequestConverter.toFriendRequest(requester, requestTarget);
        // 저장
        try {
            friendRequestRepository.save(friendRequest);
        } catch (DataIntegrityViolationException e) {
            throw new FriendException(FriendErrorCode.FRIEND_REQUEST_SAVE_CONFLICT);
        }
    }

    @Override
    public void acceptRequest(Long memberId, Long friendRequestId) {

        // 친구 요청 객체 찾기
        FriendRequest friendRequest = friendRequestRepository.findById(friendRequestId)
                .orElseThrow(() -> new FriendException(FriendErrorCode.FRIEND_REQUEST_NOT_FOUND));

        // 해당 친구 요청의 receiver가 세션 자기 자신인지 확인
        if (!friendRequest.getReceiver().getId().equals(memberId)) {
            throw new FriendException(FriendErrorCode.FRIEND_REQUEST_FORBIDDEN);
        }

        saveFriend(friendRequest);
    }

    private Member getRequestTarget(Long memberId, String email) {
        // 관리자가 아닌 유저이면서, 자기 자신이 아닌 피요청자 객체 검색
        Member requestTarget = memberRepository.findByEmailAndRoleAndIdNot(email, Role.ROLE_USER, memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 이미 친구인 경우
        boolean alreadyFriend = friendRepository.existsByMemberIdAndFriendId(memberId, requestTarget.getId());
        if (alreadyFriend) {
            throw new FriendException(FriendErrorCode.ALREADY_FRIEND);
        }

        // 이미 친구 요청이 보내진 경우
        boolean alreadyRequested = friendRequestRepository.existsBySenderIdAndReceiverId(memberId, requestTarget.getId());
        if (alreadyRequested) {
            throw new FriendException(FriendErrorCode.ALREADY_REQUESTED);
        }

        return requestTarget;
    }

    private void saveFriend(FriendRequest friendRequest) {

        Member sender = friendRequest.getSender();
        Member receiver = friendRequest.getReceiver();

        // 쌍방향 저장을 위한 친구 객체 생성
        List<Friend> friendships = List.of(
                FriendConverter.toFriend(sender, receiver),
                FriendConverter.toFriend(receiver, sender)
        );
        // 저장
        try {
            friendRepository.saveAll(friendships);
        } catch (DataIntegrityViolationException e) {
            throw new FriendException(FriendErrorCode.FRIEND_SAVE_CONFLICT);
        }
        // 저장 완료 후 요청 정보 삭제
        friendRequestRepository.delete(friendRequest);
    }
}
