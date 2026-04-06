package com.project.backend.domain.friend.converter;

import com.project.backend.domain.friend.dto.response.FriendResDTO;
import com.project.backend.domain.friend.entity.Friend;
import com.project.backend.domain.member.entity.Member;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class FriendConverter {

    public static Friend toFriend(Member member, Member friend) {
        return Friend.builder()
                .member(member)
                .opponent(friend)
                .build();
    }

    public static FriendResDTO.FriendDetailRes toFriendDetailRes(Friend friend) {
        return FriendResDTO.FriendDetailRes.builder()
                .id(friend.getId())
                .opponentName(friend.getOpponent().getNickname())
                .opponentEmail(friend.getOpponent().getEmail())
                .build();
    }

    public static FriendResDTO.FriendListRes toFriendList(List<FriendResDTO.FriendDetailRes> friendDetailResList) {
        return FriendResDTO.FriendListRes.builder()
                .friendDetailList(friendDetailResList)
                .build();
    }
}
