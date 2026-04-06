package com.project.backend.domain.friend.converter;

import com.project.backend.domain.friend.entity.Friend;
import com.project.backend.domain.member.entity.Member;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class FriendConverter {

    public static Friend toFriend(Member member, Member friend) {
        return Friend.builder()
                .member(member)
                .friend(friend)
                .build();
    }
}
