package com.project.backend.domain.suggestion.service.command;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.nlp.client.LlmClient;
import com.project.backend.domain.suggestion.converter.SuggestionConverter;
import com.project.backend.domain.suggestion.dto.request.SuggestionReqDTO;
import com.project.backend.domain.suggestion.dto.response.SuggestionResDTO;
import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.StableType;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
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
        // 반환 리스트 초기화
        List<SuggestionReqDTO.LlmSuggestionDetail> llmSuggestionReq = new ArrayList<>();
        // llm res 매핑용
        Map<Long, SuggestionCandidate> baseCandidateMap = new HashMap<>();
        // 제안 요청 개수
        int suggestionReqCnt = 0;

        // 후보 리스트를 순회하면서 제안 생성 조건을 만족하면 리스트로 반환
        for (Map.Entry<SuggestionKey, List<SuggestionCandidate>> entry : eventMap.entrySet()) {
            List<SuggestionCandidate> candidateList = entry.getValue();

            log.info("name = {}", candidateList.getFirst().title());
            // 판단 객체가 너무 적음
            if (candidateList.size() < 3) {
                continue;
            }
            // 날짜 차이 계산
            List<Integer> dayDiff = getDayDiff(candidateList);
            List<Integer> baseDiff = getBaseDiff(dayDiff);

            int trailingCnt = countTrailingSame(dayDiff);
            int baseTrailingCnt = countTrailingSame(baseDiff);
            Integer primary;
            Integer secondary = null;
            StableType stableType;

            // 완전 stable: 마지막 기준 연속 2개 이상
            // ex) [7, 7], [7, 7, 7]
            if (trailingCnt >= 2) {
                stableType = StableType.PERFECTLY_STABLE;
                primary = dayDiff.getLast();
                log.info("perfectly stable = {}, primary = {}", dayDiff, primary);
            }
            // stable이 깨진 첫 순간 (candidate 4개 이상 && dayDiff 길이 3이상)
            // ex) [7, 7, 14]
            else if (baseTrailingCnt >= 2) {
                stableType = StableType.PARTIALLY_STABLE;
                primary = baseDiff.getLast(); // 7
                secondary = dayDiff.getLast(); // 14
                log.info("partially stable = {}, primary = {}, secondary = {}", dayDiff, primary, secondary);
            }
            // 이전에 강한 패턴이 있었으나 오염된 상태 (candidate 6개 이상 && dayDiff 길이 5이상)
            // ex) [7, 7, 7, 14, 7]
            else if (candidateList.size() >= 6) {
                stableType = StableType.CONTAMINATED_STABLE;
                primary = getModeOfDayDiff(dayDiff);
                if (primary == null) {
                    log.info("unstable  dayDiff = {}, baseDiff = {}", dayDiff, baseDiff);
                    continue;
                }
                log.info("contaminated stable = {}, primary = {}", dayDiff, primary);
            }
            // 아무것도 포함되지 않으면 패턴 없음
            else {
                log.info("unstable  dayDiff = {}, baseDiff = {}", dayDiff, baseDiff);
                continue;
            }
            SuggestionCandidate baseCandidateEvent = candidateList.getLast();
            baseCandidateMap.put(baseCandidateEvent.id(), baseCandidateEvent.withDiff(primary, secondary));
            llmSuggestionReq.add(SuggestionConverter.toLlmSuggestionDetail(baseCandidateEvent, primary, secondary, stableType));
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

    private Integer getModeOfDayDiff(List<Integer> dayDiff) {

        List<Integer> recentDiff = dayDiff.subList(dayDiff.size() - 5, dayDiff.size());

        // 최근 5개의 패턴을 그룹핑
        Map<Integer, Long> freq = recentDiff.stream()
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

        // 그 중에서 가장 많은 값을 추출
        long maxCount = freq.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(-1);

        // maxCount를 가지는 Key(Diff)를 추출
        List<Integer> modes = freq.entrySet().stream()
                .filter(e -> e.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .toList();

        if (modes.size() == 1 && maxCount > recentDiff.size() / 2) {
            return modes.getFirst();
        }
        return null;
    }

    private SuggestionResDTO.LlmRes getLlmResponse(SuggestionReqDTO.LlmSuggestionReq llmReq) {
        String llmReqJson = objectMapper.writeValueAsString(llmReq);
        log.info("llm request json = {}",llmReqJson);

        String suggestionPrompt = suggestionPromptTemplate.getSuggestionPrompt();
        String userPrompt = suggestionPromptTemplate.getUserSuggestionPrompt(llmReqJson);
        String llmSuggestionRes = llmClient.chat(suggestionPrompt, userPrompt);
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

    private HashMap<SuggestionKey, List<SuggestionCandidate>> groupByTitleAndLocation(List<Event> events) {

        HashMap<SuggestionKey, List<SuggestionCandidate>> eventMap = new HashMap<>();

        for (Event event : events) {
            // title + location 조합의 키
            SuggestionKey key = SuggestionKey.from(event);
            // 만약 키가 존재하지 않는다면
            if (!eventMap.containsKey(key)) {
                // 초기화
                eventMap.put(key, new ArrayList<>());
            }
            // 이벤트 객체를 DTO로 만들어서
            SuggestionCandidate suggestionEventDetail = SuggestionCandidate.from(event);
            // 맵에 추가
            eventMap.get(key).add(suggestionEventDetail);
        }
        return eventMap;
    }

    // 각 이벤트의 날짜 차이를 리스트로 반환 [7, 7, 7, 7]
    private List<Integer> getDayDiff(List<SuggestionCandidate> suggestionEventDetails) {

        List<Integer> diff = new ArrayList<>();
        LocalDate base;
        LocalDate next;

        for (int i = 0; i < suggestionEventDetails.size() - 1; i++) {
            base = suggestionEventDetails.get(i).start().toLocalDate();
            next = suggestionEventDetails.get(i + 1).start().toLocalDate();
            diff.add((int) ChronoUnit.DAYS.between(base, next));
        }
        return diff;
    }

    private List<Integer> getBaseDiff(List<Integer> dayDiff) {
        return dayDiff.subList(0, dayDiff.size() - 1);
    }

    private int countTrailingSame(List<Integer> diffs) {

        if (diffs.isEmpty()) return 0;

        int last = diffs.getLast();
        int count = 1;

        for (int i = diffs.size() - 2; i >= 0; i--) {
            if (diffs.get(i) == last) count++;
            else break;
        }
        return count;
    }
}
