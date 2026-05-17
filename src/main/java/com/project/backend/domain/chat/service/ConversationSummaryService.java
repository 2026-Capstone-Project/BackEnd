package com.project.backend.domain.chat.service;

import com.project.backend.domain.nlp.client.LlmClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationSummaryService {

    private static final int KEEP_COUNT = 10;

    @Value("classpath:prompts/conversation-summary-prompt.txt")
    private Resource summaryPromptResource;

    private String summarySystemPrompt;

    private final LlmClient llmClient;
    private final ConversationHistoryService conversationHistoryService;

    @PostConstruct
    public void init() {
        try {
            summarySystemPrompt = new String(
                    summaryPromptResource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            log.debug("대화 요약 프롬프트 로드 완료");
        } catch (IOException e) {
            log.error("대화 요약 프롬프트 로드 실패", e);
            throw new IllegalStateException("대화 요약 프롬프트를 로드할 수 없습니다", e);
        }
    }

    @Async("vectorSyncExecutor")
    public void triggerSummaryAsync(Long memberId) {
        try {
            List<Map<String, String>> allHistory = conversationHistoryService.getHistory(memberId);
            if (allHistory.size() <= KEEP_COUNT) {
                return;
            }

            List<Map<String, String>> oldMessages = allHistory.subList(0, allHistory.size() - KEEP_COUNT);
            String existingSummary = conversationHistoryService.getSummary(memberId);

            String input = buildSummaryInput(existingSummary, oldMessages);
            String newSummary = llmClient.chat(summarySystemPrompt, input);

            conversationHistoryService.saveSummary(memberId, newSummary);
            conversationHistoryService.trimOldMessages(memberId, KEEP_COUNT);

            log.debug("대화 요약 완료 - memberId: {}, 요약 전 메시지: {}개", memberId, allHistory.size());
        } catch (Exception e) {
            log.error("대화 요약 실패 - memberId: {}", memberId, e);
        }
    }

    private String buildSummaryInput(String existingSummary, List<Map<String, String>> messages) {
        StringBuilder sb = new StringBuilder();
        if (existingSummary != null && !existingSummary.isBlank()) {
            sb.append("[기존 요약]\n").append(existingSummary).append("\n\n");
        }
        sb.append("[새 대화]\n");
        for (Map<String, String> msg : messages) {
            sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }
        return sb.toString();
    }
}