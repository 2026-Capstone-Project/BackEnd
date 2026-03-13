package com.project.backend.domain.chat.service;

import com.project.backend.domain.chat.converter.ChatConverter;
import com.project.backend.domain.chat.dto.request.ChatReqDTO;
import com.project.backend.domain.chat.dto.response.ChatResDTO;
import com.project.backend.domain.chat.exception.ChatErrorCode;
import com.project.backend.domain.chat.exception.ChatException;
import com.project.backend.domain.nlp.client.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final LlmClient llmClient;
    private final ChatPromptTemplate chatPromptTemplate;

    @Override
    public ChatResDTO.SendRes sendMessage(ChatReqDTO.SendReq reqDTO) {
        try {
            String reply = llmClient.chat(chatPromptTemplate.getSystemPrompt(), reqDTO.message());
            return ChatConverter.toSendResDTO(reply, reqDTO.conversationId());
        } catch (Exception e) {
            log.error("챗봇 응답 생성 실패", e);
            throw new ChatException(ChatErrorCode.CHAT_API_ERROR);
        }
    }
}
