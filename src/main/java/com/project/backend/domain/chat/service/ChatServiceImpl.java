package com.project.backend.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.domain.chat.converter.ChatConverter;
import com.project.backend.domain.chat.dto.request.ChatReqDTO;
import com.project.backend.domain.chat.dto.response.ChatResDTO;
import com.project.backend.domain.chat.enums.ActionType;
import com.project.backend.domain.chat.enums.ScheduleType;
import com.project.backend.domain.chat.exception.ChatErrorCode;
import com.project.backend.domain.chat.exception.ChatException;
import com.project.backend.domain.chat.function.FunctionCallHandler;
import com.project.backend.domain.chat.function.FunctionDefinitionBuilder;
import com.project.backend.domain.chat.function.ScheduleActionResult;
import com.project.backend.domain.nlp.client.FunctionCallResponse;
import com.project.backend.domain.nlp.client.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    private final FunctionDefinitionBuilder functionDefinitionBuilder;
    private final FunctionCallHandler functionCallHandler;
    private final ObjectMapper objectMapper;

    @Override
    public ChatResDTO.SendRes sendMessage(Long memberId, ChatReqDTO.SendReq reqDTO) {
        try {
            String message = reqDTO.message();

            // 1. MySQL RAG
            String mysqlContext = dateRangeExtractor.extract(message)
                    .map(range -> scheduleContextBuilder.build(memberId, range))
                    .orElse(null);

            // 2. Qdrant RAG (실패해도 채팅 동작 유지)
            String vectorContext = null;
            try {
                vectorContext = vectorContextBuilder.build(memberId, message);
            } catch (Exception e) {
                log.warn("Qdrant RAG 실패, MySQL RAG만으로 진행: {}", e.getMessage());
            }

            // pending context 주입: clarification 후속 메시지에서 대상 일정을 명시적으로 알려줌
            Map<String, String> pendingCtx    = conversationHistoryService.getPendingContext(memberId);
            Map<String, String> lastActionCtx = conversationHistoryService.getLastActionContext(memberId);
            String scheduleContext = mergeContexts(mysqlContext, vectorContext);
            String systemPrompt    = chatPromptTemplate.getSystemPrompt(scheduleContext, pendingCtx, lastActionCtx);
            List<Map<String, Object>> tools = functionDefinitionBuilder.build();

            // 3. Redis 히스토리 조회 + Map<String,String> → Map<String,Object> 변환
            // 이유: chatWithFunctions()는 tool_calls 중첩 구조를 위해 Map<String,Object> 필요
            //       Java 제네릭 불변성으로 Map<String,String>은 Map<String,Object>의 서브타입 아님
            List<Map<String, Object>> messages = convertHistory(
                    conversationHistoryService.getHistory(memberId));
            messages.add(Map.of("role", "user", "content", message));

            // 4. 1차 LLM 호출 (Function Calling)
            FunctionCallResponse llmRes = llmClient.chatWithFunctions(systemPrompt, messages, tools);

            // 5. 응답 분기
            String reply;
            ActionType action         = ActionType.NONE;
            Long scheduleId           = null;
            Long recurrenceGroupId    = null;
            ScheduleType scheduleType = null;

            if (llmRes.isClarification()) {
                // C. 되묻기 — 대상 scheduleId/scheduleType을 pending context로 저장
                reply  = parseClarificationQuestion(llmRes.functionArguments());
                action = ActionType.CLARIFYING;
                savePendingContextIfPresent(memberId, llmRes.functionArguments());

            } else {
                // B/A. CRUD 실행 또는 일반 텍스트 — pending context 소비 완료 후 삭제
                conversationHistoryService.clearPendingContext(memberId);

                if (llmRes.isFunctionCall()) {
                    // B. CRUD 실행
                    ScheduleActionResult result = functionCallHandler.handle(
                            llmRes.functionName(), llmRes.functionArguments(), memberId);

                    // 실행 결과를 tool 메시지로 추가 후 2차 LLM 호출 → 자연어 응답 생성
                    messages.add(buildAssistantToolCallMessage(llmRes));
                    messages.add(Map.of(
                            "role", "tool",
                            "tool_call_id", llmRes.toolCallId(),
                            "content", result.summary()
                    ));

                    FunctionCallResponse secondRes = llmClient.chatWithFunctions(systemPrompt, messages, tools);
                    reply = secondRes.isRespondToUser()
                            ? parseRespondToUserMessage(secondRes.functionArguments())
                            : result.summary();

                    action            = result.action();
                    scheduleId        = result.scheduleId();
                    recurrenceGroupId = result.recurrenceGroupId();
                    scheduleType      = result.scheduleType();

                    // 직전 처리 일정 저장 — 다음 턴에서 "그냥 삭제/수정" 같은 모호한 요청 처리에 사용
                    if (result.scheduleId() != null && result.scheduleType() != null) {
                        conversationHistoryService.saveLastActionContext(
                                memberId, result.scheduleId(), result.scheduleType().name());
                    }

                } else if (llmRes.isRespondToUser()) {
                    // A. 일반 텍스트 응답 (respondToUser 함수 호출 형태)
                    reply = parseRespondToUserMessage(llmRes.functionArguments());

                } else {
                    // Fallback: tool_choice:"required" 환경에서는 거의 발생하지 않음
                    reply = llmRes.textContent();
                }
            }

            // 6. Redis 히스토리 저장 — user/assistant만, tool 메시지는 저장하지 않음
            // clarification 응답은 history에 저장하지 않음:
            // "이번만? 이후전체?" 패턴이 누적되면 LLM이 단일 일정에도 같은 패턴을 반복하는 loop 발생.
            // clarification 상태는 pendingContext(Redis)가 관리하므로 history 불필요.
            conversationHistoryService.saveMessage(memberId, "user", message);
            if (action != ActionType.CLARIFYING) {
                conversationHistoryService.saveMessage(memberId, "assistant", reply);
            }

            return ChatConverter.toSendResDTO(reply, action, scheduleId, recurrenceGroupId, scheduleType);

        } catch (Exception e) {
            log.error("챗봇 응답 생성 실패", e);
            throw new ChatException(ChatErrorCode.CHAT_API_ERROR);
        }
    }

    // OpenAI 2차 호출용 assistant tool_call 메시지 구성
    // HashMap 사용 이유: content를 키에서 제외해야 함 (Map.of()는 null 값 비허용)
    // OpenAI 스펙상 tool_calls 있는 assistant 메시지는 content 생략 가능
    private Map<String, Object> buildAssistantToolCallMessage(FunctionCallResponse llmRes) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");
        msg.put("tool_calls", List.of(Map.of(
                "id", llmRes.toolCallId(),
                "type", "function",
                "function", Map.of(
                        "name", llmRes.functionName(),
                        "arguments", llmRes.functionArguments()
                )
        )));
        return msg;
    }

    private void savePendingContextIfPresent(Long memberId, String argsJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            Object rawId   = args.get("scheduleId");
            Object rawType = args.get("scheduleType");
            if (rawId != null && rawType != null) {
                Long id = ((Number) rawId).longValue();
                conversationHistoryService.savePendingContext(memberId, id, (String) rawType);
            }
        } catch (Exception e) {
            log.warn("pending context 저장 실패 (무시): {}", e.getMessage());
        }
    }

    private String parseRespondToUserMessage(String argsJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            return (String) args.get("message");
        } catch (Exception e) {
            log.error("respondToUser 파싱 실패: {}", argsJson, e);
            return "죄송해요, 응답 처리 중 오류가 발생했어요.";
        }
    }

    private String parseClarificationQuestion(String argsJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            return (String) args.get("question");
        } catch (Exception e) {
            log.error("Clarification question 파싱 실패: {}", argsJson, e);
            return "좀 더 자세히 말씀해 주시겠어요?";
        }
    }

    // Map<String, String> → Map<String, Object> 변환
    private List<Map<String, Object>> convertHistory(List<Map<String, String>> history) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, String> h : history) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", h.get("role"));
            m.put("content", h.get("content"));
            result.add(m);
        }
        return result;
    }

    private String mergeContexts(String mysqlContext, String vectorContext) {
        if (mysqlContext == null && vectorContext == null) return null;
        if (mysqlContext == null) return "[의미 유사 일정]\n" + vectorContext;
        if (vectorContext == null) return mysqlContext;
        return mysqlContext + "\n\n[의미 유사 일정]\n" + vectorContext;
    }
}