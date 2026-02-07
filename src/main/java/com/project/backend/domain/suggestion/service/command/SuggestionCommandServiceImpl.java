package com.project.backend.domain.suggestion.service.command;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.nlp.client.LlmClient;
import com.project.backend.domain.suggestion.converter.LlmSuggestionConverter;
import com.project.backend.domain.suggestion.converter.SuggestionConverter;
import com.project.backend.domain.suggestion.detector.RecurrencePatternDetector;
import com.project.backend.domain.suggestion.detector.util.RecurrencePreprocessor;
import com.project.backend.domain.suggestion.detector.vo.DetectionResult;
import com.project.backend.domain.suggestion.detector.vo.RecurrencePreprocessResult;
import com.project.backend.domain.suggestion.dto.request.SuggestionReqDTO;
import com.project.backend.domain.suggestion.dto.response.SuggestionResDTO;
import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.llm.LlmSuggestionResponseParser;
import com.project.backend.domain.suggestion.llm.SuggestionPromptTemplate;
import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import com.project.backend.domain.suggestion.vo.SuggestionCandidate;
import com.project.backend.domain.suggestion.vo.SuggestionKey;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SuggestionCommandServiceImpl implements SuggestionCommandService {

    private final EventRepository eventRepository;
    private final SuggestionRepository suggestionRepository;
    private final MemberRepository memberRepository;
    private final LlmClient llmClient;
    private final SuggestionPromptTemplate suggestionPromptTemplate;
    private final ObjectMapper objectMapper;
    private final LlmSuggestionResponseParser llmSuggestionResponseParser;
    private final RecurrencePatternDetector detector;

    @Override
    public Map<SuggestionKey, List<SuggestionCandidate>> createSuggestion(Long memberId) {

        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate oneYearAgo = now.minusYears(1);
        LocalDateTime from = oneYearAgo.atStartOfDay();
        LocalDateTime to = now.atStartOfDay().plusDays(1);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 최근 1년 간의 Event 객체
        List<Event> events = eventRepository.findByMemberIdAndInRangeAndRecurrenceGroupIsNull(memberId, from, to);
        // 최근 1년간의 Event 객체를 (이름 + 장소)로 그룹핑
        Map<SuggestionKey, List<SuggestionCandidate>> eventMap = groupByTitleAndLocation(events);
        // 제안 요청 개수
        int suggestionReqCnt = 0;
        // llm res 매핑용
        Map<Long, SuggestionCandidate> baseCandidateMap = new HashMap<>();

        List<SuggestionReqDTO.LlmSuggestionDetail> llmSuggestionReq = new ArrayList<>();

        for (Map.Entry<SuggestionKey, List<SuggestionCandidate>> entry : eventMap.entrySet()) {
            List<SuggestionCandidate> candidateList = entry.getValue();
            SuggestionCandidate baseCandidate = candidateList.getLast();

            RecurrencePreprocessResult result = RecurrencePreprocessor.preprocess(candidateList);

            Optional<DetectionResult> drOpt = detector.detect(result);

            if (drOpt.isEmpty()) {
                continue;
            }
            DetectionResult dr = drOpt.get();

            SuggestionReqDTO.LlmSuggestionDetail detail = LlmSuggestionConverter.toLlmSuggestionDetail(baseCandidate, dr);
            llmSuggestionReq.add(detail);

            baseCandidateMap.put(baseCandidate.id(),
                    baseCandidate.withPattern(
                            detail.primaryPattern(),
                            detail.secondaryPattern(),
                            detail.patternType()
                    )
            );

            suggestionReqCnt++;
        }

        SuggestionReqDTO.LlmSuggestionReq llmReq =
                SuggestionConverter.toLlmSuggestionReq(suggestionReqCnt, llmSuggestionReq);
        // llm 결과
        SuggestionResDTO.LlmRes llmRes = getLlmResponse(llmReq);
        log.info("llmRes = {}", llmRes.toString());
        saveAllSuggestion(baseCandidateMap, llmRes, member);

        return eventMap;
    }

    private SuggestionResDTO.LlmRes getLlmResponse(SuggestionReqDTO.LlmSuggestionReq llmReq) {
        String llmReqJson = objectMapper.writeValueAsString(llmReq);
        log.info("llm request json = {}",llmReqJson);

        String suggestionPrompt = suggestionPromptTemplate.getSuggestionPrompt();
        String userPrompt = suggestionPromptTemplate.getUserSuggestionPrompt(llmReqJson);
        String llmSuggestionRes = llmClient.chat(suggestionPrompt, userPrompt);
        log.info("llm response json = {}",llmSuggestionRes);
        return llmSuggestionResponseParser.parse(llmSuggestionRes);
    }

    private void saveAllSuggestion(
            Map<Long, SuggestionCandidate> baseCandidateMap,
            SuggestionResDTO.LlmRes llmRes,
            Member member
    ) {
        List<Suggestion> suggestions = llmRes.llmSuggestionList().stream()
                .flatMap(llmSuggestion -> {
                    SuggestionCandidate base = baseCandidateMap.get(llmSuggestion.eventId());
                    if (base == null) {
                        log.warn("baseCandidate가 존재하지 않음. eventId={}", llmSuggestion.eventId());
                        return Stream.empty();
                    }
                    return eventRepository.findById(base.id())
                            .map(e -> SuggestionConverter.toSuggestion(base, llmSuggestion, member, e))
                            .stream();
                })
                .toList();
        suggestionRepository.saveAll(suggestions);
    }

    private Map<SuggestionKey, List<SuggestionCandidate>> groupByTitleAndLocation(List<Event> events) {

        Map<SuggestionKey, List<SuggestionCandidate>> eventMap = new HashMap<>();

        for (Event event : events) {
            // title + location 조합의 키
            SuggestionKey key = SuggestionKey.from(event);
            // 추가
            eventMap
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(SuggestionCandidate.from(event));
        }
        eventMap.values()
                .forEach(list -> list.sort(Comparator.comparing(SuggestionCandidate::start)));
        return eventMap;
    }
}
