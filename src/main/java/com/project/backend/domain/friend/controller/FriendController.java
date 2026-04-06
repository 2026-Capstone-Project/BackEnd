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

    @GetMapping("/requests/received")
    public CustomResponse<FriendResDTO.FriendRequestListRes> getReceivedFriendRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        FriendResDTO.FriendRequestListRes resDTO = friendQueryService.getReceivedFriendRequest(customUserDetails.getId());
        return CustomResponse.onSuccess("받은 친구 요청 목록 조회 완료", resDTO);
    }

    @GetMapping()
    public CustomResponse<FriendResDTO.FriendListRes> getFriend(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        FriendResDTO.FriendListRes resDTO = friendQueryService.getFriend(customUserDetails.getId());
        return CustomResponse.onSuccess("친구 목록 조회 완료", resDTO);
    }

    @PostMapping("/requests")
    public CustomResponse<String> sendRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid FriendReqDTO.SendRequestReq reqDTO
    ) {
        friendCommandService.sendRequest(customUserDetails.getId(), reqDTO);
        return CustomResponse.onSuccess("친구 요청 완료", null);
    }

    @PostMapping("/reqeusts/{requestId}/accept")
    public CustomResponse<String> acceptFriendRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable("requestId") Long friendRequestId
    ) {
        friendCommandService.acceptRequest(customUserDetails.getId(), friendRequestId);
        return CustomResponse.onSuccess("친구 요청 수락 완료", null);
    }

    @PostMapping("/requests/{requestId}/reject")
    public CustomResponse<String> rejectFriendRequest(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long requestId
    ) {
        friendCommandService.rejectRequest(customUserDetails.getId(), requestId);
        return CustomResponse.onSuccess("친구 요청 거절 완료", null);
    }

    @DeleteMapping("/{friendId}")
    public CustomResponse<String> deleteFriend(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable Long friendId
    ) {
        friendCommandService.deleteFriend(customUserDetails.getId(), friendId);
        return CustomResponse.onSuccess("친구 삭제 완료", null);
    }
}
