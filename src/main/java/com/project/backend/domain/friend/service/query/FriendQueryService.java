package com.project.backend.domain.friend.service.query;

import com.project.backend.domain.friend.dto.response.FriendResDTO;

public interface FriendQueryService {

    FriendResDTO.FriendRequestListRes getSentFriendRequest(Long memberId);

    FriendResDTO.FriendRequestListRes getReceivedFriendRequest(Long memberId);

    FriendResDTO.FriendListRes getFriend(Long memberId);
}
