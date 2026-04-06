package com.project.backend.domain.friend.converter;

import com.project.backend.domain.friend.entity.FriendRequest;
import com.project.backend.domain.friend.enums.FriendRequestStatus;
import com.project.backend.domain.member.entity.Member;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class FriendRequestConverter {

    public static FriendRequest toFriendRequest(Member sender, Member receiver) {
        return FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();
    }
}
