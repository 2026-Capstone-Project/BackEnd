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
    private final DateRangeExtractor dateRangeExtractor;
    private final ScheduleContextBuilder scheduleContextBuilder;
    private final VectorContextBuilder vectorContextBuilder;

    @Override
    public ChatResDTO.SendRes sendMessage(Long memberId, ChatReqDTO.SendReq reqDTO) {
        try {
            String message = reqDTO.message();

            // 1. 날짜 범위 파싱 → 일정/할 일 조회 → 컨텍스트 텍스트 생성 (MySQL RAG)
            String mysqlContext = dateRangeExtractor.extract(message)
                    .map(range -> scheduleContextBuilder.build(memberId, range))
                    .orElse(null);

            // 2. Qdrant RAG(의미 기반) -> 실패해도 채팅은 동작
            String vectorContext = null;
            try {
                vectorContext = vectorContextBuilder.build(memberId, message);
            } catch (Exception e) {
                log.warn("Qdrant RAG 실패, MySQL RAG만으로 진행: {}", e.getMessage());
            }

            // 3. 두 결과 합치기 + 중복 제거
            String scheduleContext = mergeContexts(mysqlContext, vectorContext);

            // 4. 시스템 프롬프트에 일정 컨텍스트 주입
            String systemPrompt = chatPromptTemplate.getSystemPrompt(scheduleContext);

            // 5. Redis에서 기존 히스토리 조회 후 새 메시지 추가
            List<Map<String, String>> messages = new ArrayList<>(conversationHistoryService.getHistory(memberId));
            messages.add(Map.of("role", "user", "content", message));

            // 6. 히스토리 + 컨텍스트 포함 시스템 프롬프트로 OpenAI 호출
            String reply = llmClient.chatWithHistory(systemPrompt, messages);

            // 7. 유저 메시지와 GPT 응답을 Redis에 저장
            conversationHistoryService.saveMessage(memberId, "user", message);
            conversationHistoryService.saveMessage(memberId, "assistant", reply);

            return ChatConverter.toSendResDTO(reply);
        } catch (Exception e) {
            log.error("챗봇 응답 생성 실패", e);
            throw new ChatException(ChatErrorCode.CHAT_API_ERROR);
        }
    }

    private String mergeContexts(String mysqlContext, String vectorContext) {
        if (mysqlContext == null && vectorContext == null) return null;
        if (mysqlContext == null) return "[의미 유사 일정]\n" + vectorContext;
        if (vectorContext == null) return mysqlContext;

        // 둘 다 있으면 합치기
        return mysqlContext + "\n\n[의미 유사 일정]\n" + vectorContext;
    }
}
