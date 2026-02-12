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
import com.project.backend.domain.suggestion.exception.SuggestionErrorCode;
import com.project.backend.domain.suggestion.exception.SuggestionException;
import com.project.backend.domain.suggestion.llm.LlmSuggestionResponseParser;
import com.project.backend.domain.suggestion.llm.SuggestionPromptTemplate;
import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import com.project.backend.domain.suggestion.util.SuggestionAnchorUtil;
import com.project.backend.domain.suggestion.vo.RecurrenceSuggestionCandidate;
import com.project.backend.domain.suggestion.vo.RecurrenceSuggestionException;
import com.project.backend.domain.suggestion.vo.SuggestionCandidate;
import com.project.backend.domain.suggestion.vo.SuggestionKey;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceException;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import com.project.backend.domain.todo.repository.TodoRecurrenceExceptionRepository;
import com.project.backend.domain.todo.repository.TodoRecurrenceGroupRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

    private final SuggestionRepository suggestionRepository;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final TodoRepository todoRepository;
    private final TodoRecurrenceGroupRepository todoRecurrenceGroupRepository;
    private final TodoRecurrenceExceptionRepository todoRecurrenceExceptionRepository;

    private final LlmClient llmClient;
    private final SuggestionPromptTemplate suggestionPromptTemplate;
    private final LlmSuggestionResponseParser llmSuggestionResponseParser;
    private final ObjectMapper objectMapper;

    private final RecurrencePatternDetector detector;

    private final GeneratorFactory generatorFactory;
    private final EndConditionFactory endConditionFactory;

    @Override
    public void createSuggestion(Long memberId) {

//        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate now = LocalDate.of(2026, 1, 31);
        LocalDate oneYearAgo = now.minusYears(1);
        LocalDateTime from = oneYearAgo.atStartOfDay();
        LocalDateTime to = now.atStartOfDay().plusDays(1);

        // 최근 1년 간의 Event 객체
        List<Event> events = eventRepository.findByMemberIdAndInRangeAndRecurrenceGroupIsNull(memberId, from, to);
        // 최근 1년간의 Event 객체를 (이름 + 장소)로 그룹핑
        Map<SuggestionKey, List<SuggestionCandidate>> eventMap = groupByTitleAndLocation(events);

        List<Suggestion> suggestions = generateSuggestion(eventMap, memberId, now);
        if (suggestions.isEmpty()) {
            return;
        }

        saveAllSuggestion(suggestions, memberId);
    }

    @Override
    public void createTodoSuggestion(Long memberId) {
//        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        // TODO : todo일때 날짜인데, event는 시간까지 있어서 플러스 데이를 한 것인데, 이거 통일하기
        LocalDate now = LocalDate.of(2026, 2, 5);
        LocalDate from = now.minusYears(1);
        LocalDate to = now.plusDays(0);

        // 최근 1년 간의 Event 객체
        List<Todo> todos = todoRepository.findByMemberIdAndInRangeAndRecurrenceGroupIsNull(memberId, from, to);

        // 최근 1년간의 Todo 객체를 (이름 + 메모)로 그룹핑
        Map<SuggestionKey, List<SuggestionCandidate>> todoMap = groupByTitleAndMemo(todos);

        List<Suggestion> suggestions = generateSuggestion(todoMap, memberId, now);
        if (suggestions.isEmpty()) {
            return;
        }

        saveAllSuggestion(suggestions, memberId);
    }

    @Override
    public void createRecurrenceSuggestion(Long memberId) {
        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
//        LocalDate now = LocalDate.of(2026, 2, 16);

        List<RecurrenceGroup> recurrenceGroups =
                recurrenceGroupRepository.findCandidateRecurrenceGroups(memberId, now);

        Map<Long, RecurrenceSuggestionCandidate> rgMap = recurrenceGroups.stream()
                .map(RecurrenceSuggestionCandidate::from)
                .collect(Collectors.toMap(
                        RecurrenceSuggestionCandidate::id,
                        candidate -> candidate
                ));

        List<Suggestion> suggestions = generateRecurrenceSuggestion(rgMap, memberId, now);
        saveAllSuggestion(suggestions, memberId);
    }

    @Override
    public void createTodoRecurrenceSuggestion(Long memberId) {
//        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate now = LocalDate.of(2026, 2, 16);

        List<TodoRecurrenceGroup> todoRecurrenceGroups =
                todoRecurrenceGroupRepository.findCandidateTodoRecurrenceGroups(memberId, now);

        Map<Long, RecurrenceSuggestionCandidate> rgMap = todoRecurrenceGroups.stream()
                .map(RecurrenceSuggestionCandidate::from)
                .collect(Collectors.toMap(
                        RecurrenceSuggestionCandidate::id,
                        candidate -> candidate
                ));

        List<Suggestion> suggestions = generateRecurrenceSuggestion(rgMap, memberId, now);
        saveAllSuggestion(suggestions, memberId);
    }

    // TODO : 락 구현하기?
    @Override
    public void acceptSuggestion(Long memberId, Long suggestionId) {

        Suggestion suggestion = suggestionRepository.findByIdAndActiveIsTrue(suggestionId)
                .orElseThrow(() -> new SuggestionException(SuggestionErrorCode.SUGGESTION_NOT_FOUND));

        suggestion.accept();

        // 생성 로직 구현하기
    }

    // TODO : 락 구현하기?
    @Override
    public void rejectSuggestion(Long memberId, Long suggestionId) {

        Suggestion suggestion = suggestionRepository.findByIdAndActiveIsTrue(suggestionId)
                .orElseThrow(() -> new SuggestionException(SuggestionErrorCode.SUGGESTION_NOT_FOUND));

        suggestion.reject();

        // 생성 로직 구현하기
    }

    private List<Suggestion> generateSuggestion(Map<SuggestionKey, List<SuggestionCandidate>> allCandidateMap, Long memberId, LocalDate now) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        // 제안 객체 생성을 위한 정보 저장 맵
        Map<Long, SuggestionCandidate> baseCandidateMap = new HashMap<>();
        // LLM 응답 정보 저장 리스트
        List<SuggestionReqDTO.LlmSuggestionDetail> llmSuggestionReq = new ArrayList<>();
        // 같은 규칙으로 묶인 객체 순회
        for (Map.Entry<SuggestionKey, List<SuggestionCandidate>> entry : allCandidateMap.entrySet()) {
            List<SuggestionCandidate> candidateList = entry.getValue();
            SuggestionCandidate baseCandidate = candidateList.getLast();

            // 후보 객체를 입력하여 패턴 전처리 결과 반환
            RecurrencePreprocessResult result = RecurrencePreprocessor.preprocess(candidateList);
            // 전처리 결과로 패턴 탐지 결과 옵셔널 객체로 반환
            Optional<DetectionResult> drOpt = detector.detect(result);
            // 탐지 결과가 비어있다 -> 패턴이 없다
            if (drOpt.isEmpty()) {
                continue;
            }
            DetectionResult dr = drOpt.get();
            // 기준 후보 객체와 패턴 탐지 결과로 LLM 제안 요청 DTO 변환
            SuggestionReqDTO.LlmSuggestionDetail detail = LlmSuggestionConverter.toLlmSuggestionDetail(baseCandidate, dr);
            // 패턴 탐지 결과로 예측한 다음 객체 생성일
            // TODO : 앵커 데이트 날짜에 이미 객체가 있다면 컨티뉴해서 llm 호출 줄이기
            LocalDate anchorDate = SuggestionAnchorUtil.computeAnchorDate(
                    baseCandidate.start(),
                    detail.patternType(),
                    detail.primaryPattern()
            );
            log.info("anchorDate = {}", anchorDate);
            // 제안을 생성하기 위한 최소 시점 계산 (N_INTERVAL : dayDiff가 7일 미만인 경우 dayDiff가 leadDays)
            Integer leadDays = SuggestionAnchorUtil.computeLeadDays(detail.patternType(), detail.primaryPattern());
            log.info("leadDay = {}", leadDays);
            // 명시적 NPE 방지
            if (anchorDate == null || leadDays == null) {
                continue;
            }
            // 다음 객체 생성일이 현재 서버 시간 + leadDays가 아니면 아직 제안을 생성할 시점이 아님
            if (!anchorDate.equals(now.plusDays(leadDays))) {
                continue;
            }
            // LLM 요청 바디에 추가
            llmSuggestionReq.add(detail);
            // 제안 객체 생성을 위한 정보 추가
            baseCandidateMap.put(
                    baseCandidate.id(),
                    baseCandidate.withPattern(detail.primaryPattern(), detail.secondaryPattern())
            );
        }
        // LLM 요청 바디를 List 형식의 DTO로 변환
        SuggestionReqDTO.LlmSuggestionReq llmReq =
                SuggestionConverter.toLlmSuggestionReq(llmSuggestionReq.size(), llmSuggestionReq);
        // 요청 바디가 비어있다면 빈 리스트 반환
        if (llmSuggestionReq.isEmpty()) {
            return Collections.emptyList();
        }
        // LLM 응답 바디를 DTO로 변환
        SuggestionResDTO.LlmRes llmRes = getLlmResponse(llmReq);
        // LLM 응답 바디와 제안 객체 생성을 위해 저장한 baseCandidate를 활용하여 List<Suggestion> 타입으로 반환
        return llmRes.llmSuggestionList().stream()
                .flatMap(llmSuggestion -> {
                    SuggestionCandidate base = baseCandidateMap.get(llmSuggestion.id());
                    if (base == null) {
                        log.warn("baseCandidate가 존재하지 않음. event/todoId={}", llmSuggestion.id());
                        return Stream.empty();
                    }
                    return switch (base.category()) {
                        case EVENT -> eventRepository.findById(base.id())
                                .map(e -> SuggestionConverter.toSuggestion(base, llmSuggestion, member, e, null))
                                .stream();

                        case TODO -> todoRepository.findById(base.id())
                                .map(t -> SuggestionConverter.toSuggestion(base, llmSuggestion, member, null, t))
                                .stream();
                    };
                })
                .toList();
    }

    private List<Suggestion> generateRecurrenceSuggestion(
            Map<Long, RecurrenceSuggestionCandidate> allCandidateMap,
            Long memberId,
            LocalDate now

    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        List<SuggestionReqDTO.LlmRecurrenceGroupSuggestionDetail> llmSuggestionReq = new ArrayList<>();

        Map<Long, RecurrenceSuggestionCandidate> baseRecurrenceGroupMap = new HashMap<>();
        // 같은 규칙으로 묶인 객체 순회
        for (Map.Entry<Long, RecurrenceSuggestionCandidate> entry : allCandidateMap.entrySet()) {
            RecurrenceSuggestionCandidate candidate = entry.getValue();

            LocalDateTime last = calcLastStart(candidate);
            if (last == null) continue; // 실제 남는 occurrence가 없음(전부 SKIP 등)

            // 마지막 반복이 시작되는 날로부터 7일전
            if (last.toLocalDate().isEqual(now.plusDays(7))) {
                llmSuggestionReq.add(SuggestionConverter.toLlmRecurrenceGroupSuggestionDetail(candidate, last));

                baseRecurrenceGroupMap.put(candidate.id(), candidate);
            }
        }

        SuggestionReqDTO.LlmRecurrenceGroupSuggestionReq llmReq =
                SuggestionConverter.toLlmRecurrenceGroupSuggestionReq(llmSuggestionReq.size(), llmSuggestionReq);

        SuggestionResDTO.LlmRecurrenceGroupSuggestionRes llmRes = getLlmResponse(llmReq);
        log.info("llmRes = {}", llmRes.toString());

        return llmRes.llmRecurrenceGroupSuggestionList().stream()
                .flatMap(llmRecurrenceGroupSuggestion -> {
                    RecurrenceSuggestionCandidate base = baseRecurrenceGroupMap.get(llmRecurrenceGroupSuggestion.id());
                    if (base == null) {
                        log.warn("baseRG가 존재하지 않음. rgId={}", llmRecurrenceGroupSuggestion.id());
                        return Stream.empty();
                    }
                    return switch (base.category()) {
                        case EVENT -> recurrenceGroupRepository.findById(base.id())
                                .map(rg -> SuggestionConverter.toSuggestion(base, llmRecurrenceGroupSuggestion, member, rg, null))
                                .stream();

                        case TODO -> todoRecurrenceGroupRepository.findById(base.id())
                                .map(trg -> SuggestionConverter.toSuggestion(base, llmRecurrenceGroupSuggestion, member, null, trg))
                                .stream();
                    };
                })
                .toList();
    }

    private LocalDateTime calcLastStart(RecurrenceSuggestionCandidate candidate) {

        Generator generator = generatorFactory.getGenerator(candidate);
        EndCondition endCondition = endConditionFactory.getEndCondition(candidate);

        LocalDateTime current = candidate.startDate();

        Map<LocalDate, RecurrenceSuggestionException> exMap = getExMap(candidate);

        int count = 1;

        LocalDateTime last = resolveExceptionStart(current, exMap.get(current.toLocalDate()));

        // expandEvents랑 동일한 count 흐름:
        // base(1) 처리 후, while에서 next를 만들고 count++ 해서 2,3,... 진행
        while (endCondition.shouldContinue(current, count, candidate)) {

            current = generator.next(current, candidate);

            if (candidate.getEndDate() != null && current.toLocalDate().isAfter(candidate.getEndDate())) {
                break;
            }
            if (count > 20000) {
                break; // 안전장치
            }
            // 모든 정지 조건을 탈출했다면 갱신
            if (!exMap.isEmpty()) {
                LocalDateTime start = resolveExceptionStart(current, exMap.get(current.toLocalDate()));
                // SKIP or Exception 없음
                if (start != null) {
                    log.info("title = {}, start = {}", candidate.event().getTitle(), start);
                    last = start;
                }
            }
            else {
                last = current;
            }
            count++;
        }
        // 전부 스킵이면 null 가능
        return last;
    }

    private Map<LocalDate, RecurrenceSuggestionException> getExMap(RecurrenceSuggestionCandidate candidate) {
        return switch (candidate.category()) {
            case EVENT -> {
                List<RecurrenceException> exList =
                        recurrenceExceptionRepository.findByRecurrenceGroupId(candidate.id());
                yield exList.stream()
                        .collect(Collectors.toMap(
                                        RecurrenceException::getExceptionDate,
                                        RecurrenceSuggestionException::from
                                )
                        );
            }
            case TODO -> {
                List<TodoRecurrenceException> exList =
                        todoRecurrenceExceptionRepository.findByTodoRecurrenceGroupId(candidate.id());
                yield exList.stream()
                        .collect(Collectors.toMap(
                                        TodoRecurrenceException::getExceptionDate,
                                        RecurrenceSuggestionException::from
                                )
                        );
            }
        };
    }

    private LocalDateTime resolveExceptionStart(LocalDateTime current, RecurrenceSuggestionException ex) {
        if (ex == null) return current;
        if (ex.exceptionType() == SKIP) return null;
        if (ex.exceptionType() == OVERRIDE) return ex.startTime();
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

    private void saveAllSuggestion(List<Suggestion> suggestions, Long memberId) {
        List<byte[]> hashes = suggestions.stream()
                .map(Suggestion::getTargetKeyHash)
                .toList();
        Set<String> existingHexHash = suggestionRepository.findExistingActiveTargetKeyHashes(memberId, hashes)
                .stream()
                .map(this::getHexHash)
                .collect(Collectors.toSet());
        List<Suggestion> toSave = suggestions.stream()
                .filter(s -> !existingHexHash.contains(getHexHash(s.getTargetKeyHash())))
                .toList();
        if (toSave.isEmpty()) {
            return;
        }
        try {
            suggestionRepository.saveAll(toSave);
        } catch (DataIntegrityViolationException e) {
            log.error("Suggestion 중복 저장 memberId = {}", memberId);
        }
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

    private Map<SuggestionKey, List<SuggestionCandidate>> groupByTitleAndMemo(List<Todo> todos) {
        Map<SuggestionKey, List<SuggestionCandidate>> todoMap = new HashMap<>();

        for (Todo todo : todos) {
            // title + memo 조합의 키
            SuggestionKey key = SuggestionKey.from(todo);
            // 추가
            todoMap
                    .computeIfAbsent(key, k -> new ArrayList<>())
                    .add(SuggestionCandidate.from(todo));
        }
        todoMap.values()
                .forEach(list -> list.sort(Comparator.comparing(SuggestionCandidate::start)));
        return todoMap;
    }

    // 해시 메서드
    private String getHexHash(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}
