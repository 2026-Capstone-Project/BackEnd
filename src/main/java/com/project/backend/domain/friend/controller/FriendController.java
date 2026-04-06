package com.project.backend.domain.friend.controller;

import com.project.backend.domain.friend.dto.request.FriendReqDTO;
import com.project.backend.domain.friend.dto.response.FriendResDTO;
import com.project.backend.domain.friend.service.command.FriendCommandService;
import com.project.backend.domain.friend.service.query.FriendQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/friends")
public class FriendController {

    private final FriendCommandService friendCommandService;
    private final FriendQueryService friendQueryService;

    @GetMapping("/requests/sent")
    public CustomResponse<FriendResDTO.FriendRequestListRes> getSentFriendRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        FriendResDTO.FriendRequestListRes resDTO = friendQueryService.getSentFriendRequest(customUserDetails.getId());
        return CustomResponse.onSuccess("보낸 친구 요청 목록 조회 완료", resDTO);
    }

    @PostMapping("/request")
    public CustomResponse<String> sendRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid FriendReqDTO.SendRequestReq reqDTO
    ) {
        friendCommandService.sendRequest(customUserDetails.getId(), reqDTO);
        return CustomResponse.onSuccess("친구 요청 완료", null);
    }
}
