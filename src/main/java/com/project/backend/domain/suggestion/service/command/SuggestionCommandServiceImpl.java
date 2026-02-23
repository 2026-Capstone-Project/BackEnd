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
import com.project.backend.domain.suggestion.enums.Status;
import com.project.backend.domain.suggestion.exception.SuggestionErrorCode;
import com.project.backend.domain.suggestion.exception.SuggestionException;
import com.project.backend.domain.suggestion.executor.SuggestionExecutor;
import com.project.backend.domain.suggestion.executor.SuggestionExecutorFactory;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
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
    private final SuggestionExecutorFactory suggestionExecutorFactory;

    private final GeneratorFactory generatorFactory;
    private final EndConditionFactory endConditionFactory;

    @Override
    public void createEventSuggestion(Long memberId) {

        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
//        LocalDate now = LocalDate.of(2026, 1, 19);
        LocalDate oneYearAgo = now.minusYears(1);
        LocalDateTime from = oneYearAgo.atStartOfDay();
        LocalDateTime to = now.atStartOfDay().plusYears(1);

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

        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
        // TODO : todo일때 날짜인데, event는 시간까지 있어서 플러스 데이를 한 것인데, 이거 통일하기
//        LocalDate now = LocalDate.of(2026, 1, 28);
        LocalDate from = now.minusYears(1);
        LocalDate to = now.plusYears(1);

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
//        LocalDate now = LocalDate.of(2026, 3, 2);

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

        LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
//        LocalDate now = LocalDate.of(2026, 2, 16);

        List<TodoRecurrenceGroup> todoRecurrenceGroups =
                todoRecurrenceGroupRepository.findCandidateTodoRecurrenceGroups(memberId, now);

        Map<Long, RecurrenceSuggestionCandidate> trgMap = todoRecurrenceGroups.stream()
                .map(RecurrenceSuggestionCandidate::from)
                .collect(Collectors.toMap(
                        RecurrenceSuggestionCandidate::id,
                        candidate -> candidate
                ));

        List<Suggestion> suggestions = generateRecurrenceSuggestion(trgMap, memberId, now);
        saveAllSuggestion(suggestions, memberId);
    }

    @Override
    public void acceptSuggestion(Long memberId, Long suggestionId) {

        Suggestion suggestion = suggestionRepository.findForExecute(suggestionId, memberId)
                        .orElseThrow(() -> new SuggestionException(SuggestionErrorCode.SUGGESTION_NOT_FOUND));

        Status currentStatus = suggestion.getStatus();
        if (currentStatus == Status.ACCEPTED || currentStatus == Status.REJECTED) {
            throw new SuggestionException(SuggestionErrorCode.SUGGESTION_NOT_FOUND);
        }
        // 원자적 수락
        int updated = suggestionRepository.acceptAtomically(suggestionId, memberId, currentStatus);
        if (updated == 0) {
            throw new SuggestionException(SuggestionErrorCode.SUGGESTION_CONFLICT);
        }

        SuggestionExecutor executor = suggestionExecutorFactory.getExecutor(suggestion.getSuggestionType());

        executor.execute(suggestion, currentStatus, memberId);
    }

    @Override
    public void rejectSuggestion(Long memberId, Long suggestionId) {
        // 원자적 거절
        // 1) PRIMARY -> SECONDARY 전환 (secondaryContent 있을 때만)
        int switched = suggestionRepository.rejectPrimaryToSecondary(memberId, suggestionId);
        if (switched == 1) {
            return;
        }

        // 2) (PRIMARY인데 secondary 없음) OR (SECONDARY 상태) -> REJECTED
        int rejected = suggestionRepository.rejectFinally(memberId, suggestionId);
        if (rejected == 1) {
            return;
        }

        // 없거나(active=false) 이미 처리됨
        throw new SuggestionException(SuggestionErrorCode.SUGGESTION_NOT_FOUND);
    }

    @Override
    public void delete(Long memberId) {
        suggestionRepository.deleteAllByMemberId(memberId);
    }

    @Override
    public void createSuggestion(Long memberId) {
        createEventSuggestion(memberId);
        createTodoSuggestion(memberId);
        createRecurrenceSuggestion(memberId);
        createTodoRecurrenceSuggestion(memberId);
    }

    private List<Suggestion> generateSuggestion(
            Map<SuggestionKey, List<SuggestionCandidate>> allCandidateMap,
            Long memberId,
            LocalDate now
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        // 제안 객체 생성을 위한 정보 저장 맵
        Map<Long, SuggestionCandidate> baseCandidateMap = new LinkedHashMap<>();
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
            List<LocalDate> primaryAnchorDate = SuggestionAnchorUtil.computeAnchorDate(
                    baseCandidate.start(),
                    detail.patternType(),
                    detail.primaryPattern()
            );
            List<LocalDate> secondaryAnchorDate = null;
            if (detail.secondaryPattern() != null) {
                secondaryAnchorDate = SuggestionAnchorUtil.computeAnchorDate(
                        baseCandidate.start(),
                        detail.patternType(),
                        detail.secondaryPattern()
                );
            }
            log.debug("anchorDate = {}", primaryAnchorDate);
            // 제안을 생성하기 위한 최소 시점 계산 (N_INTERVAL : dayDiff가 7일 미만인 경우 dayDiff가 leadDays)
            Integer leadDays = SuggestionAnchorUtil.computeLeadDays(detail.patternType(), detail.primaryPattern());
            log.debug("leadDay = {}", leadDays);
            // 명시적 NPE 방지
            if (primaryAnchorDate == null || leadDays == null) {
                continue;
            }
            // 다음 객체 생성일이 현재 서버 시간 + leadDays가 아니면 아직 제안을 생성할 시점이 아님
            if (!primaryAnchorDate.getFirst().equals(now.plusDays(leadDays))) {
                continue;
            }
            // 다음 객체 생성일에 이미 객체가 있는 경우 제안 건너뛰기
            if (checkAlreadyExist(primaryAnchorDate, baseCandidate, member)
                    || checkAlreadyExist(secondaryAnchorDate, baseCandidate, member)) {
                continue;
            }
            // LLM 요청 바디에 추가
            llmSuggestionReq.add(detail);
            // 제안 객체 생성을 위한 정보 추가
            baseCandidateMap.put(
                    baseCandidate.id(),
                    baseCandidate.withAnchor(primaryAnchorDate, secondaryAnchorDate)
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
                    log.debug("anchor = {}", base.primaryAnchorDate());
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
        // 제안 객체 생성을 위한 정보 저장 맵
        Map<Long, RecurrenceSuggestionCandidate> baseRecurrenceGroupMap = new HashMap<>();
        // LLM 응답 정보 저장 리스트
        List<SuggestionReqDTO.LlmRecurrenceGroupSuggestionDetail> llmSuggestionReq = new ArrayList<>();
        // 같은 규칙으로 묶인 객체 순회
        for (Map.Entry<Long, RecurrenceSuggestionCandidate> entry : allCandidateMap.entrySet()) {
            RecurrenceSuggestionCandidate candidate = entry.getValue();
            // 해당 반복 객체에서 발생할 수 있는 가장 마지막 날 반환
            LocalDateTime last = calcLastStart(candidate);
            // 마지막 날이 안나옴 -> 전부 SKIP 등
            if (last == null) {
                continue;
            }
            // 마지막 반복이 시작되는 날로부터 7일전이 아니면 생성할 시점이 아님
            Integer leadDays = SuggestionAnchorUtil.computeLeadDays(candidate);
            if (!last.toLocalDate().isEqual(now.plusDays(leadDays))) {
                continue;
            }
            // LLM 요청 바디에 추가
            llmSuggestionReq.add(SuggestionConverter.toLlmRecurrenceGroupSuggestionDetail(candidate, last));
            // 제안 객체 생성을 위한 정보 추가
            baseRecurrenceGroupMap.put(candidate.id(), candidate);
        }
        // LLM 요청 바디를 List 형식의 DTO로 변환
        SuggestionReqDTO.LlmRecurrenceGroupSuggestionReq llmReq =
                SuggestionConverter.toLlmRecurrenceGroupSuggestionReq(llmSuggestionReq.size(), llmSuggestionReq);
        // 요청 바디가 비어있다면 빈 리스트 반환
        if (llmSuggestionReq.isEmpty()) {
            return Collections.emptyList();
        }
        // LLM 응답 바디를 DTO로 변환
        SuggestionResDTO.LlmRecurrenceGroupSuggestionRes llmRes = getLlmResponse(llmReq);
        // LLM 응답 바디와 제안 객체 생성을 위해 저장한 baseCandidate를 활용하여 List<Suggestion> 타입으로 반환
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
        // 생성기, 종료 조건 초기화
        Generator generator = generatorFactory.getGenerator(candidate);
        EndCondition endCondition = endConditionFactory.getEndCondition(candidate);

        LocalDateTime current = candidate.startDate();
        // 반복 예외 가져오기
        Map<LocalDateTime, RecurrenceSuggestionException> exMap = getExMap(candidate);
        // 맨 처음이 반복 예외인 경우 SKIP, OVERRIDE 처리
        LocalDateTime last = resolveExceptionStart(current, exMap.get(current));

        int count = 1;
        // 반복 조건에 따라 반복
        while (endCondition.shouldContinue(current, count, candidate)) {
            // 다음 시간
            current = generator.next(current, candidate);
            // 반복 객체의 종료 시간을 넘어선 경우
            if (candidate.getEndDate() != null && current.toLocalDate().isAfter(candidate.getEndDate())) {
                break;
            }
            // 무한 반복 방지
            if (count > 20000) {
                break;
            }
            // 반복 예외가 있다면
            if (!exMap.isEmpty() && exMap.containsKey(current)) {
                // SKIP, OVERRIDE 적용
                LocalDateTime start = resolveExceptionStart(current, exMap.get(current));
                // SKIP or Exception 없음
                if (start != null) {
                    log.debug("title = {}, start = {}", candidate.event().getTitle(), start);
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

    // candidate의 카테고리에 따라 알맞은 반복 예외를 반환
    private Map<LocalDateTime, RecurrenceSuggestionException> getExMap(RecurrenceSuggestionCandidate candidate) {

        return switch (candidate.category()) {
            case EVENT -> {
                List<RecurrenceException> exList =
                        recurrenceExceptionRepository.findAllByRecurrenceGroupId(candidate.id());
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
                                        TodoRecurrenceException::getExceptionDateTime,
                                        RecurrenceSuggestionException::from
                                )
                        );
            }
        };
    }

    // 반복 예외 처리기
    private LocalDateTime resolveExceptionStart(LocalDateTime current, RecurrenceSuggestionException ex) {

        if (ex == null) return current;
        if (ex.exceptionType() == SKIP) return null;
        if (ex.exceptionType() == OVERRIDE) return ex.startTime();
        return current;
    }

    private SuggestionResDTO.LlmRes getLlmResponse(SuggestionReqDTO.LlmSuggestionReq llmReq) {

        String llmReqJson = objectMapper.writeValueAsString(llmReq);
        log.debug("llm request json = {}",llmReqJson);

        String suggestionPrompt = suggestionPromptTemplate.getSuggestionPrompt();
        String userPrompt = suggestionPromptTemplate.getUserSuggestionPrompt(llmReqJson);
        String llmSuggestionRes = llmClient.chat(suggestionPrompt, userPrompt);
        log.debug("llm response json = {}",llmSuggestionRes);
        return llmSuggestionResponseParser.parseSuggestion(llmSuggestionRes);
    }

    private SuggestionResDTO.LlmRecurrenceGroupSuggestionRes getLlmResponse(SuggestionReqDTO.LlmRecurrenceGroupSuggestionReq llmReq) {

        String llmReqJson = objectMapper.writeValueAsString(llmReq);
        log.debug("llm request json = {}",llmReqJson);

        String suggestionPrompt = suggestionPromptTemplate.getRecurrenceSuggestionPrompt();
        String userPrompt = suggestionPromptTemplate.getUserSuggestionPrompt(llmReqJson);
        String llmSuggestionRes = llmClient.chat(suggestionPrompt, userPrompt);
        log.debug("llm response json = {}",llmSuggestionRes);
        return llmSuggestionResponseParser.parseRecurrenceGroupSuggestion(llmSuggestionRes);
    }

    private boolean checkAlreadyExist(List<LocalDate> anchors, SuggestionCandidate candidate, Member member) {
        if (anchors == null || anchors.isEmpty()) return false;

        Long memberId = member.getId();
        String title = candidate.title();
        String content = candidate.content();
        LocalTime time = candidate.start().toLocalTime();

        return switch (candidate.category()) {
            case EVENT -> anchors.stream()
                    .anyMatch(date -> eventRepository.existsByMemberIdAndTitleAndLocationAndStartTime(
                            memberId, title, content, date.atTime(time)
                    ));
            case TODO -> anchors.stream()
                    .anyMatch(date -> todoRepository.existsByMemberIdAndTitleAndMemoAndStartDateAndDueTime(
                            memberId, title, content, date, time
                    ));
        };
    }

    // 중복된 제안을 해시를 통해 비교 후, 걸러낸 List<Suggestion>을 한 번에 저장
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

    // 해시 메서드
    private String getHexHash(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    // 헬퍼 메서드
    private <T> Map<SuggestionKey, List<SuggestionCandidate>> groupBy(
             List<T> items,
             Function<T, SuggestionKey> keyExtractor,
             Function<T, SuggestionCandidate> candidateMapper
    ) {
        Map<SuggestionKey, List<SuggestionCandidate>> map = new LinkedHashMap<>();

        for (T item : items) {
            SuggestionKey key = keyExtractor.apply(item);
            map.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(candidateMapper.apply(item));
        }
        return map;
    }

    private  Map<SuggestionKey, List<SuggestionCandidate>> groupByTitleAndLocation(
             List<Event> events
    ) {
        return groupBy(events, SuggestionKey::from, SuggestionCandidate::from);
    }

    private  Map<SuggestionKey, List<SuggestionCandidate>> groupByTitleAndMemo(
             List<Todo> todos
    ) {
        return groupBy(todos, SuggestionKey::from, SuggestionCandidate::from);
    }
}
