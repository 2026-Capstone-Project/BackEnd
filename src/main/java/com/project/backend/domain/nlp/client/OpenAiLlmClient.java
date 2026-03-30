package com.project.backend.domain.nlp.client;

import com.project.backend.domain.nlp.exception.NlpErrorCode;
import com.project.backend.domain.nlp.exception.NlpException;
import com.project.backend.global.config.LlmConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.llm.provider", havingValue = "openai")
public class OpenAiLlmClient implements LlmClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final LlmConfig llmConfig;
    private final WebClient webClient;

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", llmConfig.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3
        );

        try {
            log.debug("OpenAI API 호출 - model: {}", llmConfig.getModel());

            Map response = webClient.post()
                    .uri(OPENAI_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + llmConfig.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new NlpException(NlpErrorCode.LLM_API_ERROR);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
            String result = (String) message.get("content");

            log.debug("OpenAI API 응답 수신 완료");
            return result;

        } catch (WebClientResponseException e) {
            log.error("OpenAI API 호출 실패 - status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new NlpException(NlpErrorCode.LLM_API_ERROR);
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패", e);
            throw new NlpException(NlpErrorCode.LLM_API_ERROR);
        }
    }

    @Override
    public String chatWithHistory(String systemPrompt, List<Map<String, String>> messages) {
        List<Map<String, Object>> messageBody = new ArrayList<>();
        messageBody.add(Map.of("role", "system", "content", systemPrompt));
        messages.forEach(m -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("role", m.get("role"));
            entry.put("content", m.get("content"));
            messageBody.add(entry);
        });

        Map<String, Object> requestBody = Map.of(
                "model", llmConfig.getModel(),
                "messages", messageBody,
                "temperature", 0.7
        );

        try {
            log.debug("OpenAI API 호출 (멀티턴) - model: {}, 히스토리 크기: {}", llmConfig.getModel(), messages.size());

            Map response = webClient.post()
                    .uri(OPENAI_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + llmConfig.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new NlpException(NlpErrorCode.LLM_API_ERROR);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
            String result = (String) message.get("content");

            log.debug("OpenAI API 응답 수신 완료 (멀티턴)");
            return result;

        } catch (WebClientResponseException e) {
            log.error("OpenAI API 호출 실패 (멀티턴) - status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new NlpException(NlpErrorCode.LLM_API_ERROR);
        } catch (Exception e) {
            log.error("OpenAI API 호출 실패 (멀티턴)", e);
            throw new NlpException(NlpErrorCode.LLM_API_ERROR);
        }
    }

    @Override
    public FunctionCallResponse chatWithFunctions(String systemPrompt,
                                                  List<Map<String, Object>> messages,
                                                  List<Map<String, Object>> tools)
    {
        List<Map<String, Object>> messageBody = new ArrayList<>();
        messageBody.add(Map.of("role", "system", "content", systemPrompt));
        messages.addAll(messages);

        // tool_choice: "auto" -> LLM이 스스로 함수 호출 여부를 판단
        // temperature: 0.3 -> 파라미터 추출 정확도 우선
        Map<String, Object> requestBody = Map.of(
                "model", llmConfig.getModel(),
                "messages", messageBody,
                "tools", tools,
                "tool_choice", "auto",
                "temperature", 0.3
        );

        try {
            log.debug("OpenAI Function Calling 호출 - tools: {}개", tools.size());
            Map<?, ?> response = callApi(requestBody);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");

            // tool_calls 존재 여부로 function call / 일반 텍스트 분기
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");

            if (toolCalls != null && !toolCalls.isEmpty()) {
                // Function Call 응답
                Map<String, Object> toolCall = toolCalls.getFirst();
                String toolCallId = (String) toolCall.get("id");

                @SuppressWarnings("unchecked")
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String functionName      = (String) function.get("name");
                String functionArguments = (String) function.get("arguments");

                log.debug("Function Call 감지 - name: {}, toolCallId: {}", functionName, toolCallId);
                return new FunctionCallResponse(null, functionName, toolCallId, functionArguments);
            }

            // 일반 텍스트 응답
            String textContent = (String) message.get("content");
            return new FunctionCallResponse(textContent, null, null, null);

        } catch (WebClientResponseException e) {
            log.error("OpenAI Function Calling 실패 - status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new NlpException(NlpErrorCode.LLM_API_ERROR);
        } catch (Exception e) {
            log.error("OpenAI Function Calling 실패", e);
            throw new NlpException(NlpErrorCode.LLM_API_ERROR);
        }
    }

    // ─────────────────────────────────────────
    // 공통 WebClient API 호출
    // ─────────────────────────────────────────
    private Map<?, ?> callApi(Map<String, Object> requestBody) {
        Map<?, ?> response = webClient.post()
                .uri(OPENAI_API_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + llmConfig.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) {
            throw new NlpException(NlpErrorCode.LLM_API_ERROR);
        }
        return response;
    }

    // choices[0].message.content 추출
    @SuppressWarnings("unchecked")
    private String extractTextContent(Map<?, ?> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
        return (String) message.get("content");
    }
}
