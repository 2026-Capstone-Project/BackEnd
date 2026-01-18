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
}
