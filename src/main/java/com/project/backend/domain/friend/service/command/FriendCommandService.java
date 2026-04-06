package com.project.backend.domain.friend.service.command;

import com.project.backend.domain.friend.dto.request.FriendReqDTO;

public interface FriendCommandService {

    void sendRequest(Long memberId, FriendReqDTO.SendRequestReq reqDTO);

    void acceptRequest(Long memberId, Long friendRequestId);
}
