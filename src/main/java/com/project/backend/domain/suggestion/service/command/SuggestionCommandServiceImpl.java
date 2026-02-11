package com.project.backend.domain.suggestion.service.command;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.factory.EndConditionFactory;
import com.project.backend.domain.event.factory.GeneratorFactory;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.repository.RecurrenceExceptionRepository;
import com.project.backend.domain.event.repository.RecurrenceGroupRepository;
import com.project.backend.domain.event.strategy.endcondition.EndCondition;
import com.project.backend.domain.event.strategy.generator.Generator;
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
import com.project.backend.domain.suggestion.util.SuggestionAnchorUtil;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.project.backend.domain.event.enums.ExceptionType.OVERRIDE;
import static com.project.backend.domain.event.enums.ExceptionType.SKIP;

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
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final GeneratorFactory generatorFactory;
    private final EndConditionFactory endConditionFactory;

    @Override
    public void createSuggestion(Long memberId) {

        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
//        LocalDate now = LocalDate.of(2026, 1, 31);
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

            LocalDate anchorDate = SuggestionAnchorUtil.computeAnchorDate(
                    baseCandidate.start(),
                    detail.patternType(),
                    detail.primaryPattern()
            );

            log.info("anchorDate = {}", anchorDate);
            // 제안으로 생성될 객체의 시작 시간으로부터 7일 전 (dayDiff가 7일 미만인 경우 dayDiff가 leadDays)
            Integer leadDays = SuggestionAnchorUtil.computeLeadDays(detail.patternType(), detail.primaryPattern());
            log.info("leadDay = {}", leadDays);
            if (anchorDate == null || leadDays == null) {
                continue;
            }

            if (!anchorDate.equals(now.plusDays(leadDays))) {
                continue;
            }

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
                SuggestionConverter.toLlmSuggestionReq(llmSuggestionReq.size(), llmSuggestionReq);
        // llm 결과
        if (!llmSuggestionReq.isEmpty()) {
            SuggestionResDTO.LlmRes llmRes = getLlmResponse(llmReq);
            log.info("llmRes = {}", llmRes.toString());
            saveAllSuggestion(baseCandidateMap, llmRes, member);
        }
    }

    @Override
    public void createRecurrenceSuggestion(Long memberId) {
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
//        LocalDate now = LocalDate.of(2026, 2, 16);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        List<RecurrenceGroup> recurrenceGroups =
                recurrenceGroupRepository.findCandidateRecurrenceGroups(memberId, now);

        List<SuggestionReqDTO.LlmRecurrenceGroupSuggestionDetail> llmSuggestionReq = new ArrayList<>();

        Map<Long, RecurrenceGroup> baseRecurrenceGroupMap = new HashMap<>();

        for (RecurrenceGroup rg : recurrenceGroups) {

            LocalDateTime last = calculateLastVisibleOccurrence(rg);
            if (last == null) continue; // 실제 남는 occurrence가 없음(전부 SKIP 등)

            // 마지막 반복이 시작되는 날로부터 7일전
            if (last.toLocalDate().isEqual(now.plusDays(7))) {
                log.info("last title = {}, last = {}", rg.getEvent().getTitle(), last);
                llmSuggestionReq.add(SuggestionConverter.toLlmRecurrenceGroupSuggestionDetail(rg, last));

                baseRecurrenceGroupMap.put(rg.getId(), rg);
            }
        }
        SuggestionReqDTO.LlmRecurrenceGroupSuggestionReq llmReq =
                SuggestionConverter.toLlmRecurrenceGroupSuggestionReq(llmSuggestionReq.size(), llmSuggestionReq);

        if (!llmSuggestionReq.isEmpty()) {
            SuggestionResDTO.LlmRecurrenceGroupSuggestionRes llmRes = getLlmResponse(llmReq);
            log.info("llmRes = {}", llmRes.toString());

            saveAllSuggestion(baseRecurrenceGroupMap, llmRes, member);
        }
    }

    private LocalDateTime calculateLastVisibleOccurrence(RecurrenceGroup rg) {

        Event baseEvent = rg.getEvent();
        if (baseEvent == null) return null;

        Generator generator = generatorFactory.getGenerator(rg);
        EndCondition endCondition = endConditionFactory.getEndCondition(rg);

        List<RecurrenceException> exList =
                recurrenceExceptionRepository.findByRecurrenceGroupId(rg.getId());

        Map<LocalDate, RecurrenceException> exMap = exList.stream()
                .collect(Collectors.toMap(
                        RecurrenceException::getExceptionDate,
                        ex -> ex
                ));

        LocalDateTime current = baseEvent.getStartTime();
        int count = 1;

        LocalDateTime last = resolveEffectiveStart(current, exMap.get(current.toLocalDate()));

        // expandEvents랑 동일한 count 흐름:
        // base(1) 처리 후, while에서 next를 만들고 count++ 해서 2,3,... 진행
        while (endCondition.shouldContinue(current, count, rg)) {

            current = generator.next(current, rg);

            if (rg.getEndDate() != null && current.toLocalDate().isAfter(rg.getEndDate())) {
                break;
            }
            if (count > 20000) {
                break; // 안전장치
            }
            // 모든 정지 조건을 탈출했다면 갱신
            if (!exMap.isEmpty()) {
                LocalDateTime effective = resolveEffectiveStart(current, exMap.get(current.toLocalDate()));
                if (effective != null) {
                log.info("title = {}, effective = {}", rg.getEvent().getTitle(), effective);
                    last = effective;
                }
            } else {
                last = current;
            }
            count++;
        }

        // 전부 스킵이면 null 가능
        return last;
    }

    private LocalDateTime resolveEffectiveStart(LocalDateTime current, RecurrenceException ex) {
        if (ex == null) return current;
        if (ex.getExceptionType() == SKIP) return null;
        if (ex.getExceptionType() == OVERRIDE) return ex.getStartTime();
        return current;
    }

    private SuggestionResDTO.LlmRes getLlmResponse(SuggestionReqDTO.LlmSuggestionReq llmReq) {
        String llmReqJson = objectMapper.writeValueAsString(llmReq);
        log.info("llm request json = {}",llmReqJson);

        String suggestionPrompt = suggestionPromptTemplate.getSuggestionPrompt();
        String userPrompt = suggestionPromptTemplate.getUserSuggestionPrompt(llmReqJson);
        String llmSuggestionRes = llmClient.chat(suggestionPrompt, userPrompt);
        log.info("llm response json = {}",llmSuggestionRes);
        return llmSuggestionResponseParser.parseSuggestion(llmSuggestionRes);
    }

    private SuggestionResDTO.LlmRecurrenceGroupSuggestionRes getLlmResponse(SuggestionReqDTO.LlmRecurrenceGroupSuggestionReq llmReq) {
        String llmReqJson = objectMapper.writeValueAsString(llmReq);
        log.info("llm request json = {}",llmReqJson);

        String suggestionPrompt = suggestionPromptTemplate.getRecurrenceSuggestionPrompt();
        String userPrompt = suggestionPromptTemplate.getUserSuggestionPrompt(llmReqJson);
        String llmSuggestionRes = llmClient.chat(suggestionPrompt, userPrompt);
        log.info("llm response json = {}",llmSuggestionRes);
        return llmSuggestionResponseParser.parseRecurrenceGroupSuggestion(llmSuggestionRes);
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

    private void saveAllSuggestion(
            Map<Long, RecurrenceGroup> baseRecurrenceGroupMap,
            SuggestionResDTO.LlmRecurrenceGroupSuggestionRes llmRes,
            Member member
    ) {
        List<Suggestion> suggestions = llmRes.llmRecurrenceGroupSuggestionList().stream()
                .flatMap(llmRecurrenceGroupSuggestion -> {
                    RecurrenceGroup base = baseRecurrenceGroupMap.get(llmRecurrenceGroupSuggestion.recurrenceGroupId());
                    if (base == null) {
                        log.warn("baseRG가 존재하지 않음. rgId={}", llmRecurrenceGroupSuggestion.recurrenceGroupId());
                        return Stream.empty();
                    }
                    return Stream.of(SuggestionConverter.toSuggestion(base, llmRecurrenceGroupSuggestion, member));
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
