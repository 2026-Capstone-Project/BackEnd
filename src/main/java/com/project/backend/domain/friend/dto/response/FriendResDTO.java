package com.project.backend.domain.friend.dto.response;

import lombok.Builder;

import java.util.List;

public class FriendResDTO {

    @Builder
    public record FriendRequestDetailRes(
            Long id,
            String senderName,
            String receiverName
    ) {
    }

    @Builder
    public record FriendRequestListRes(
        List<FriendRequestDetailRes> friendRequestDetailList
    ) {
    }
}
