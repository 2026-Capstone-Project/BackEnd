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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final int HISTORY_THRESHOLD = 20;
    private static final int RECENT_HISTORY_COUNT = 10;

    private final LlmClient llmClient;
    private final ChatPromptTemplate chatPromptTemplate;
    private final ConversationHistoryService conversationHistoryService;
    private final ConversationSummaryService conversationSummaryService;
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

            Map<String, String> pendingCtx    = conversationHistoryService.getPendingContext(memberId);
            Map<String, String> lastActionCtx = conversationHistoryService.getLastActionContext(memberId);
            String scheduleContext = mergeContexts(mysqlContext, vectorContext);
            String systemPrompt    = chatPromptTemplate.getSystemPrompt(scheduleContext, pendingCtx, lastActionCtx);
            List<Map<String, Object>> tools = functionDefinitionBuilder.build();

            // 3. 요약본 + 최근 10개 구조로 히스토리 구성
            List<Map<String, Object>> messages = buildHistoryWithSummary(memberId);
            String llmMessage = chatPromptTemplate.replaceRelativeDates(message);
            messages.add(Map.of("role", "user", "content", llmMessage));

            // 4. 1차 LLM 호출 (Function Calling)
            FunctionCallResponse llmRes = llmClient.chatWithFunctions(systemPrompt, messages, tools);

            // 5. 응답 분기
            String reply;
            ActionType action         = ActionType.NONE;
            Long scheduleId           = null;
            Long recurrenceGroupId    = null;
            ScheduleType scheduleType = null;

            if (llmRes.isClarification()) {
                reply  = parseClarificationQuestion(llmRes.functionArguments());
                action = ActionType.CLARIFYING;
                savePendingContextIfPresent(memberId, llmRes.functionArguments());

            } else {
                conversationHistoryService.clearPendingContext(memberId);

                if (llmRes.isFunctionCall()) {
                    ScheduleActionResult result = functionCallHandler.handle(
                            llmRes.functionName(), llmRes.functionArguments(), memberId);

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

                    if (result.scheduleId() != null && result.scheduleType() != null) {
                        if (result.action() == ActionType.CLARIFYING) {
                            conversationHistoryService.savePendingContext(
                                    memberId, result.scheduleId(), result.scheduleType().name());
                        } else {
                            conversationHistoryService.saveLastActionContext(
                                    memberId, result.scheduleId(), result.scheduleType().name());
                        }
                    }

                } else if (llmRes.isRespondToUser()) {
                    reply = parseRespondToUserMessage(llmRes.functionArguments());

                } else {
                    reply = llmRes.textContent();
                }
            }

            // 6. Redis 히스토리 저장
            conversationHistoryService.saveMessage(memberId, "user", message);
            if (action != ActionType.CLARIFYING) {
                conversationHistoryService.saveMessage(memberId, "assistant", reply);
            }

            // 7. 임계값 초과 시 요약 트리거 (JPA 커밋 후 비동기 실행)
            if (conversationHistoryService.getHistorySize(memberId) > HISTORY_THRESHOLD) {
                afterCommit(() -> conversationSummaryService.triggerSummaryAsync(memberId));
            }

            return ChatConverter.toSendResDTO(reply, action, scheduleId, recurrenceGroupId, scheduleType);

        } catch (Exception e) {
            log.error("챗봇 응답 생성 실패", e);
            throw new ChatException(ChatErrorCode.CHAT_API_ERROR);
        }
    }

    // 요약본 + 최근 N개 구조로 히스토리 구성
    // 요약본이 있으면 system 메시지로 첫 번째 원소에 추가해 LLM이 높은 우선순위로 처리하도록 함
    private List<Map<String, Object>> buildHistoryWithSummary(Long memberId) {
        List<Map<String, Object>> result = new ArrayList<>();

        String summary = conversationHistoryService.getSummary(memberId);
        if (summary != null && !summary.isBlank()) {
            Map<String, Object> summaryMsg = new HashMap<>();
            summaryMsg.put("role", "system");
            summaryMsg.put("content", "[이전 대화 요약]\n" + summary);
            result.add(summaryMsg);
        }

        result.addAll(convertHistory(
                conversationHistoryService.getRecentHistory(memberId, RECENT_HISTORY_COUNT)));
        return result;
    }

    private void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

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
