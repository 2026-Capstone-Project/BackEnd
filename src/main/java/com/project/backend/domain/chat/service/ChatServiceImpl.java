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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final LlmClient llmClient;
    private final ChatPromptTemplate chatPromptTemplate;
    private final ConversationHistoryService conversationHistoryService;

    @Override
    public ChatResDTO.SendRes sendMessage(Long memberId, ChatReqDTO.SendReq reqDTO) {
        try {
            // 1. Redis에서 기존 히스토리 조회
            List<Map<String, String>> messages = new ArrayList<>(conversationHistoryService.getHistory(memberId));
            messages.add(Map.of("role", "user", "content", reqDTO.message()));

            // 2. 히스토리 + 새 메시지 합쳐서 OpenAI 호출
            String reply = llmClient.chatWithHistory(chatPromptTemplate.getSystemPrompt(), messages);

            // 3. 유저 메시지와 GPT 응답을 Redis에 저장
            conversationHistoryService.saveMessage(memberId, "user", reqDTO.message());
            conversationHistoryService.saveMessage(memberId, "assistant", reply);

            return ChatConverter.toSendResDTO(reply);
        } catch (Exception e) {
            log.error("챗봇 응답 생성 실패", e);
            throw new ChatException(ChatErrorCode.CHAT_API_ERROR);
        }
    }
}
