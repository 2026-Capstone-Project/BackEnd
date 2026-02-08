package com.project.backend.domain.nlp.service.impl;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.nlp.client.LlmClient;
import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.dto.response.NlpResDTO;
import com.project.backend.domain.nlp.enums.ItemType;
import com.project.backend.domain.nlp.service.LlmResponseParser;
import com.project.backend.domain.nlp.service.NlpSaverService;
import com.project.backend.domain.nlp.service.NlpService;
import com.project.backend.domain.nlp.service.PromptTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlpServiceImpl implements NlpService {

    private final LlmClient llmClient;
    private final PromptTemplate promptTemplate;
    private final LlmResponseParser llmResponseParser;
    private final NlpSaverService nlpSaverService;
    private final MemberRepository memberRepository;

    @Override
    public NlpResDTO.ParseRes parse(NlpReqDTO.ParseReq reqDTO, Long memberId) {
        LocalDate baseDate = reqDTO.baseDate() != null ? reqDTO.baseDate() : LocalDate.now();

        String systemPrompt = promptTemplate.getSystemPrompt(baseDate);
        String userPrompt = promptTemplate.getUserPrompt(reqDTO.text());
        log.debug("LLM 파싱 요청 - memberId: {}, text: {}", memberId, reqDTO.text());

        String llmResponse = llmClient.chat(systemPrompt, userPrompt);
        return llmResponseParser.parse(llmResponse);
    }

    @Override
    @Transactional
    public NlpResDTO.ConfirmRes confirm(NlpReqDTO.ConfirmReq reqDTO, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        List<NlpResDTO.ConfirmResult> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (NlpReqDTO.ConfirmItem item : reqDTO.items()) {
            try {
                Long savedId = nlpSaverService.save(item, member);
                results.add(NlpResDTO.ConfirmResult.success(savedId, item.type(), item.title(), item.isRecurring()));
                successCount++;
                log.debug("저장 성공 - type: {}, title: {}, savedId: {}", item.type(), item.title(), savedId);
            } catch (Exception e) {
                results.add(NlpResDTO.ConfirmResult.failure(item.type(), item.title(), e.getMessage()));
                failCount++;
                log.error("저장 실패 - type: {}, title: {}", item.type(), item.title(), e);
            }
        }

        return NlpResDTO.ConfirmRes.builder()
                .totalCount(reqDTO.items().size())
                .successCount(successCount)
                .failCount(failCount)
                .results(results)
                .message(buildResultMessage(results))
                .build();
    }

    private String buildResultMessage(List<NlpResDTO.ConfirmResult> results) {
        int totalCreated = (int) results.stream()
                .filter(NlpResDTO.ConfirmResult::success)
                .count();

        long failCount = results.stream()
                .filter(r -> !r.success())
                .count();

        if (failCount == 0) {
            if (totalCreated == 1) {
                ItemType type = results.get(0).type();
                return type == ItemType.EVENT
                        ? "일정이 등록되었어요!"
                        : "할 일이 등록되었어요!";
            } else {
                return totalCreated + "개 일정이 등록되었어요!";
            }
        } else {
            return totalCreated + "개 등록, " + failCount + "개 실패했어요.";
        }
    }
}
