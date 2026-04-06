package com.project.backend.domain.friend.converter;

import com.project.backend.domain.friend.dto.response.FriendResDTO;
import com.project.backend.domain.friend.entity.FriendRequest;
import com.project.backend.domain.friend.enums.FriendRequestStatus;
import com.project.backend.domain.member.entity.Member;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class FriendRequestConverter {

    public static FriendRequest toFriendRequest(Member sender, Member receiver) {
        return FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();
    }

    public static FriendResDTO.FriendRequestDetailRes toFriendRequestDetailRes(
            FriendRequest friendRequest,
            Member sender,
            Member receiver
    ) {
        return FriendResDTO.FriendRequestDetailRes.builder()
                .id(friendRequest.getId())
                .senderName(sender.getNickname())
                .receiverName(receiver.getNickname())
                .build();
    }

    public static FriendResDTO.FriendRequestListRes toFriendRequestList(
            List<FriendResDTO.FriendRequestDetailRes> friendRequestDetailList
    ) {
        return FriendResDTO.FriendRequestListRes.builder()
                .friendRequestDetailList(friendRequestDetailList)
                .build();
    }
}
