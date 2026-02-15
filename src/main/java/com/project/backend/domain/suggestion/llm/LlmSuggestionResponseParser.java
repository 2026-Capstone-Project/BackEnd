package com.project.backend.domain.suggestion.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.domain.nlp.exception.NlpErrorCode;
import com.project.backend.domain.nlp.exception.NlpException;
import com.project.backend.domain.suggestion.dto.response.SuggestionResDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// TODO : nlp 패키지에 있는 내용을 일단 개발을 위해서 중복 작성
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmSuggestionResponseParser {

    private final ObjectMapper objectMapper;

    public SuggestionResDTO.LlmRes parseSuggestion(String llmResponse) {
        try {
            String jsonStr = extractJson(llmResponse);
            return objectMapper.readValue(jsonStr, SuggestionResDTO.LlmRes.class);

        } catch (JsonProcessingException e) {
            log.error("LLM 응답 파싱 실패: {}", llmResponse, e);
            throw new NlpException(NlpErrorCode.LLM_PARSE_ERROR);
        }
    }

    public SuggestionResDTO.LlmRecurrenceGroupSuggestionRes parseRecurrenceGroupSuggestion(String llmResponse) {
        try {
            String jsonStr = extractJson(llmResponse);
            return objectMapper.readValue(jsonStr, SuggestionResDTO.LlmRecurrenceGroupSuggestionRes.class);

        } catch (JsonProcessingException e) {
            log.error("LLM 응답 파싱 실패: {}", llmResponse, e);
            throw new NlpException(NlpErrorCode.LLM_PARSE_ERROR);
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}") + 1;

        if (start == -1 || end == 0) {
            throw new NlpException(NlpErrorCode.LLM_PARSE_ERROR);
        }

        return trimmed.substring(start, end);
    }
}
