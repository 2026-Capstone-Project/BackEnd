package com.project.backend.domain.friend.dto.response;

import lombok.Builder;

import java.util.List;

public class FriendResDTO {

    @Builder
    public record FriendRequestDetailRes(
            Long id,
            String opponentName,
            String opponentEmail
    ) {
    }

    @Builder
    public record FriendRequestListRes(
        List<FriendRequestDetailRes> friendRequestDetailList
    ) {
    }

    @Builder
    public record FriendDetailRes(
            Long id,
            String opponentName,
            String opponentEmail
    ) {
    }

    @Builder
    public record FriendListRes(
            List<FriendDetailRes> friendDetailList
    ) {
    }
}
