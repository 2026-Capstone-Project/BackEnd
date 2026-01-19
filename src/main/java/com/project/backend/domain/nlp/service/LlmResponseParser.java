package com.project.backend.domain.nlp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.domain.nlp.converter.NlpConverter;
import com.project.backend.domain.nlp.dto.response.LlmResDTO;
import com.project.backend.domain.nlp.dto.response.NlpResDTO;
import com.project.backend.domain.nlp.exception.NlpErrorCode;
import com.project.backend.domain.nlp.exception.NlpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmResponseParser {

    private final ObjectMapper objectMapper;

    public NlpResDTO.ParseRes parse(String llmResponse) {
        try {
            String jsonStr = extractJson(llmResponse);
            LlmResDTO llmResDTO = objectMapper.readValue(jsonStr, LlmResDTO.class);

            if (llmResDTO.isSingleItem()) {
                NlpResDTO.ParsedItem item = NlpConverter.toParsedItem(llmResDTO);
                return NlpResDTO.ParseRes.single(item);
            } else {
                List<NlpResDTO.ParsedItem> items = llmResDTO.items().stream()
                        .map(NlpConverter::toParsedItem)
                        .toList();
                return NlpResDTO.ParseRes.multiple(items);
            }
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
