package com.project.backend.domain.friend.service.query;

import com.project.backend.domain.friend.converter.FriendRequestConverter;
import com.project.backend.domain.friend.dto.response.FriendResDTO;
import com.project.backend.domain.friend.entity.FriendRequest;
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


    @Override
    public FriendResDTO.FriendRequestListRes getSentFriendRequest(Long memberId) {

        // 세션 멤버 객체 조회
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 세션 자기 자신이 보낸 친구 요청 리스트 조회
        List<FriendRequest> friendRequests = friendRequestRepository.findBySenderId(memberId);

        // List<FriendRequest> -> List<FriendRequestDetailRes>
        List<FriendResDTO.FriendRequestDetailRes> friendRequestDetailResList =
                friendRequests.stream()
                        .map(friendRequest -> { // 각각의 요청에 대하여 receiver 정보를 조회해 dto에 담기
                            Member receiver = memberRepository.findById(friendRequest.getReceiver().getId())
                                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
                            return FriendRequestConverter.toFriendRequestDetailRes(friendRequest, sender, receiver);
                        })
                        .toList();

        return FriendRequestConverter.toFriendRequestList(friendRequestDetailResList);
    }
}
