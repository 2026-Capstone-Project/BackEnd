package com.project.backend.domain.chat.service;

import com.project.backend.domain.chat.dto.request.ChatReqDTO;
import com.project.backend.domain.chat.dto.response.ChatResDTO;

public interface ChatService {
    ChatResDTO.SendRes sendMessage(Long memberId, ChatReqDTO.SendReq reqDTO);
}
