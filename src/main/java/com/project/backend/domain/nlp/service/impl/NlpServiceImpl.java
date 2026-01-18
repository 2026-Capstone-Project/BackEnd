package com.project.backend.domain.nlp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.domain.nlp.client.LlmClient;
import com.project.backend.domain.nlp.converter.NlpConverter;
import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.dto.response.LlmResDTO;
import com.project.backend.domain.nlp.dto.response.NlpResDTO;
import com.project.backend.domain.nlp.exception.NlpErrorCode;
import com.project.backend.domain.nlp.exception.NlpException;
import com.project.backend.domain.nlp.service.NlpService;
import com.project.backend.domain.nlp.service.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlpServiceImpl implements NlpService {

    private final ObjectMapper objectMapper;
    private final LlmClient llmClient;
    private final PromptTemplate promptTemplate;

    @Override
    public NlpResDTO.ParseRes parse(NlpReqDTO.ParseReq reqDTO, Long memberId) {
        LocalDate baseDate = reqDTO.baseDate() != null ? reqDTO.baseDate() : LocalDate.now();

        String systemPrompt = promptTemplate.getSystemPrompt(baseDate); // AI에게 주는 메뉴얼
        String userPrompt = promptTemplate.getUserPrompt(reqDTO.text()); // 유저의 실제 요청
        log.info("LLM 파싱 요청 - memberId: {}, text: {}", memberId, reqDTO.text());

        String llmResponse = llmClient.chat(systemPrompt, userPrompt);
        return parseLlmResponse(llmResponse);
    }

    private NlpResDTO.ParseRes parseLlmResponse(String llmResponse) {
        try {
            String jsonStr = extractJson(llmResponse);
            LlmResDTO llmResDTO = objectMapper.readValue(jsonStr, LlmResDTO.class);

            if (llmResDTO.isSingleItem()) {
                // 단일 항목 -> LlmResDTO 자체를 반환함
                NlpResDTO.ParsedItem item = NlpConverter.toParsedItem(llmResDTO);
                return NlpResDTO.ParseRes.single(item);
            } else {
                // 복수 항목 -> items 배열의 각 LlmParsedItem을 반환함
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
