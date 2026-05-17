package com.project.backend.domain.friend.dto;

public record FriendIdMapping(
        Long friendId,
        Long opponentMemberId
) {
}
