package com.project.backend.domain.friend.service.query;

import com.project.backend.domain.friend.converter.FriendConverter;
import com.project.backend.domain.friend.converter.FriendRequestConverter;
import com.project.backend.domain.friend.dto.response.FriendResDTO;
import com.project.backend.domain.friend.entity.Friend;
import com.project.backend.domain.friend.entity.FriendRequest;
import com.project.backend.domain.friend.repository.FriendRepository;
import com.project.backend.domain.friend.repository.FriendRequestRepository;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendQueryServiceImpl implements FriendQueryService{

    private final MemberRepository memberRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;

    @Override
    public FriendResDTO.FriendRequestListRes getSentFriendRequest(Long memberId) {
        // 세션 멤버 객체 조회
        Member me = getMember(memberId);
        // 세션 자기 자신이 보낸 친구 요청 리스트 조회
        List<FriendRequest> friendRequests = friendRequestRepository.findBySenderId(memberId);
        return buildFriendRequestList(friendRequests, me);
    }

    @Override
    public FriendResDTO.FriendRequestListRes getReceivedFriendRequest(Long memberId) {
        // 세션 멤버 객체 조회
        Member me = getMember(memberId);
        // 세션 자기 자신이 받은 친구 요청 리스트 조회
        List<FriendRequest> friendRequests = friendRequestRepository.findByReceiverId(memberId);
        return buildFriendRequestList(friendRequests, me);
    }

    @Override
    public FriendResDTO.FriendListRes getFriend(Long memberId) {
        // 모든 친구 조회
        List<Friend> friendList = friendRepository.findByMemberId(memberId);
        // List<Friend> -> List<FriendDetailRes>
        List<FriendResDTO.FriendDetailRes> detailResList = friendList.stream()
                .map(FriendConverter::toFriendDetailRes)
                .toList();

        return FriendConverter.toFriendList(detailResList);
    }

    // id로 멤버 객체를 조회
    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private FriendResDTO.FriendRequestListRes buildFriendRequestList(
            List<FriendRequest> friendRequests,
            Member me
    ) {
        List<FriendResDTO.FriendRequestDetailRes> detailResList = friendRequests.stream()
                .map(friendRequest -> {
                    // friendRequest의 receiver가 자기 자신인가?
                    boolean sentByMe = friendRequest.getSender().getId().equals(me.getId());
                    // 만약 나라면 상대를 receiver로 설정, 내가 아니라면 상대를 sender로 설정
                    Member opponent = sentByMe ? friendRequest.getReceiver() : friendRequest.getSender();
                    // 상대 정보를 기준으로 detail 생성
                    return FriendRequestConverter.toFriendRequestDetailRes(friendRequest, opponent);
                })
                .toList();

        return FriendRequestConverter.toFriendRequestList(detailResList);
    }
}
