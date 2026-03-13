package com.project.backend.domain.chat.controller;

import com.project.backend.domain.chat.dto.request.ChatReqDTO;
import com.project.backend.domain.chat.dto.response.ChatResDTO;
import com.project.backend.domain.chat.service.ChatService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("")
    public CustomResponse<ChatResDTO.SendRes> sendMessage(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                          @Valid @RequestBody ChatReqDTO.SendReq reqDTO) {
        return CustomResponse.onSuccess("챗봇 응답 성공", chatService.sendMessage(reqDTO));
    }
}
