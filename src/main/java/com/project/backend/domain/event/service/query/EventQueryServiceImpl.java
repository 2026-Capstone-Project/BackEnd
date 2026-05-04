package com.project.backend.domain.event.service.query;

import com.project.backend.domain.event.enums.InviteStatus;
import com.project.backend.domain.occurrence.dto.TodayOccurrenceResult;
import com.project.backend.domain.event.converter.EventConverter;
import com.project.backend.domain.event.converter.EventHistoryConverter;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.factory.EndConditionFactory;
import com.project.backend.domain.event.factory.GeneratorFactory;
import com.project.backend.domain.event.repository.*;
import com.project.backend.domain.event.service.EventOccurrenceResolver;
import com.project.backend.domain.event.strategy.endcondition.EndCondition;
import com.project.backend.domain.event.strategy.generator.Generator;
import com.project.backend.domain.event.validator.EventValidator;
import com.project.backend.domain.occurrence.dto.NextOccurrenceResult;
import com.project.backend.domain.reminder.enums.TargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static com.project.backend.domain.common.recurrence.enums.ExceptionType.OVERRIDE;
import static com.project.backend.domain.common.recurrence.enums.ExceptionType.SKIP;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryServiceImpl implements EventQueryService {

    private static final int MAX_OCCURRENCE_ITERATION = 20_000;

    private final EventRepository eventRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;

    private final GeneratorFactory generatorFactory;
    private final EndConditionFactory endConditionFactory;
    private final EventValidator eventValidator;
    private final EventOccurrenceResolver eventOccurrenceResolver;
    private final EventTitleHistoryRepository eventTitleHistoryRepository;
    private final EventParticipantRepository eventParticipantRepository;

    @Override
    public EventResDTO.DetailRes getEventDetail(Long eventId, LocalDateTime occurrenceDate, Long memberId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        boolean isOwner = Objects.equals(event.getMember().getId(), memberId);

        boolean isAcceptedParticipant = eventParticipantRepository
                .existsByEventIdAndMemberIdAndStatus(eventId, memberId, InviteStatus.ACCEPTED);

        if (!isOwner && !isAcceptedParticipant) {
            throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
        }
        eventValidator.validateRead(event, occurrenceDate);

        // 찾고자 하는 것이 부모 이벤트인 경우
        if (!event.isRecurring()) {
            return EventConverter.toDetailRes(event);
        }

        return eventOccurrenceResolver.resolveForRead(event, occurrenceDate);
    }

    @Override
    public EventResDTO.EventsListRes getEvents(Long memberId, LocalDate startDate, LocalDate endDate) {

        // 2026-01-01 -> 2026-01-01T00:00:00.000000000
        LocalDateTime startRange = startDate.atStartOfDay();
        // 2026-01-02 -> 2026-01-02T23:59:59.999999999
        LocalDateTime endRange = endDate.atTime(LocalTime.MAX);

        // 범위에 맞는 내가 소유자인 이벤트 목록 조회
        List<Event> OwnedEvents = getOwnedEvents(memberId, startRange, endRange);
        // 범위에 맞는 내가 참여한 이벤트 목록 조회
        List<Event> SharedEvents = getSharedEvents(memberId, startRange, endRange);
        // 두 목록 병합
        List<Event> result = concatEventList(OwnedEvents, SharedEvents);

        // 최상위 이벤트 확장
        List<EventResDTO.DetailRes> eventsListRes = expandEvents(result, startRange, endRange);

        // 시작 날짜 기준으로 정렬
        eventsListRes.sort(
                Comparator.comparing(EventResDTO.DetailRes::start)
        );

        return EventConverter.toEventsListRes(eventsListRes);
    }

    @Override
    public EventResDTO.EventTitleHistoryRes getEventTitleHistory(Long memberId, String keyword) {
        List<String> titleHistory;

        if (keyword == null || keyword.isBlank()) {
            titleHistory = eventTitleHistoryRepository.findTitleHistoryByMemberId(memberId);
        } else {
            titleHistory = eventTitleHistoryRepository.findTitleHistoryByMemberIdAndKeyword(memberId, keyword);
        }

        return EventHistoryConverter.toEventTitleHistoryRes(titleHistory);
    }

    /**
     * 기존에 존재햇던 반복 그룹을 대상으로, 해당 반복 일정에 대한 occurrenceTime의 다음 계산된 시간을 구하되,
     * 현재 시간보다 이후의 계산된 시간인지 확인한다. (단순히 다음 계산 값이 있는지)
     **/
    @Override
    public NextOccurrenceResult calculateNextOccurrence(Long eventId, LocalDateTime occurrenceTime) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        RecurrenceGroup rg = event.getRecurrenceGroup();

        if (rg == null) {
            return NextOccurrenceResult.none();
        }

        return findNextOccurrenceAfter(event, rg, occurrenceTime, LocalDateTime.now());
    }

    /**
        * 새로 생성된 반복 그룹을 대상으로, 현재 시간보다 이후의 계산된 시간이 있는지 계산 (처음부터 현재 시간보다 이후의 계산된 시간이 있는지 반환)
     **/
    @Override
    public LocalDateTime findNextOccurrenceAfterNow(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        if (!event.isRecurring()) {
            throw new EventException(EventErrorCode.NOT_RECURRING_EVENT);
        }

        return findFirstOccurrenceOnOrAfter(event, now);
    }

    /**
     * 일정에 대한 반복을 진행해 계산된 일정의 startTime이 오늘인 일정 정보 조회
     **/
    @Override
    public List<TodayOccurrenceResult> calculateTodayOccurrence(List<Long> eventIds, LocalDate currentDate) {
        List<TodayOccurrenceResult> results = new ArrayList<>();

        for (Long eventId : eventIds) {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

            results.add(calculateTodayOccurrence(event, currentDate));
        }

        return results;
    }

    //=========================================== private method ======================================================

    // 범위에 맞는 내가 소유자인 이벤트 목록 조회
    private List<Event> getOwnedEvents(Long memberId, LocalDateTime startRange, LocalDateTime endRange) {
        // 범위에 맞는 단일 이벤트 목록
        List<Event> baseEvents = eventRepository.findByMemberIdAndOverlappingRange(memberId, startRange, endRange);
        log.debug("baseEvents = {}", baseEvents);
        // 범위에 활성화 되어 있는 반복 그룹 목록
        List<RecurrenceGroup> baseRg = recurrenceGroupRepository.findActiveRecurrenceGroups(memberId, startRange.toLocalDate());
        log.debug("baseRg = {}", baseRg);
        // 활성화된 반복 그룹에서 이벤트 객체 분리 후 리스트화
        List<Event> EventFromRg = baseRg.stream()
                .map(RecurrenceGroup::getEvent)
                .toList();

        return concatEventList(baseEvents, EventFromRg);
    }

    // 범위에 맞는 내가 참여한 이벤트 목록 조회
    private List<Event> getSharedEvents(Long memberId, LocalDateTime startRange, LocalDateTime endRange) {
        // 범위에 맞는 참여한 이벤트 목록
        List<Event> baseParticipantEvents =
                eventParticipantRepository.findByMemberIdAndOverlappingRange(
                        memberId, startRange, endRange, InviteStatus.ACCEPTED);
        log.debug("baseParticipantEvents = {}", baseParticipantEvents);
        // 범위에 활성화 되어 있는 참여한 반복 그룹 목록
        List<RecurrenceGroup> baseParticipantRg =
                eventParticipantRepository.findSharedActiveRecurrenceGroups(
                        memberId, startRange.toLocalDate(), InviteStatus.ACCEPTED);
        // 활성화된 반복 그룹에서 이벤트 객체 분리 후 리스트화
        List<Event> EventFromSharedRg = baseParticipantRg.stream()
                .map(RecurrenceGroup::getEvent)
                .toList();
        log.debug("EventFromSharedRg = {}", EventFromSharedRg);

        return concatEventList(baseParticipantEvents, EventFromSharedRg);
    }

    private TodayOccurrenceResult calculateTodayOccurrence(Event event, LocalDate currentDate) {
        if (!event.isRecurring()) {
            return calculateSingleEventTodayOccurrence(event, currentDate);
        }

        return calculateRecurringEventTodayOccurrence(event, currentDate);
    }

    private TodayOccurrenceResult calculateSingleEventTodayOccurrence(Event event, LocalDate currentDate) {
        if (event.getStartTime().toLocalDate().isEqual(currentDate)) {
            return TodayOccurrenceResult.of(
                    event.getTitle(),
                    event.getStartTime().toLocalTime(),
                    TargetType.EVENT
            );
        }

        return TodayOccurrenceResult.none();
    }

    private TodayOccurrenceResult calculateRecurringEventTodayOccurrence(Event event, LocalDate currentDate) {
        RecurrenceGroup recurrenceGroup = event.getRecurrenceGroup();
        List<RecurrenceException> exceptions =
                recurrenceExceptionRepository.findAllByRecurrenceGroupId(recurrenceGroup.getId());

        Optional<TodayOccurrenceResult> movedOverrideResult =
                resolveMovedToTodayOverride(event, currentDate, exceptions);
        if (movedOverrideResult.isPresent()) {
            return movedOverrideResult.get();
        }

        LocalDateTime occurrenceTime = findOccurrenceOnDate(event, currentDate);
        if (occurrenceTime == null) {
            return TodayOccurrenceResult.none();
        }

        return resolveOccurrenceResult(event, currentDate, occurrenceTime, exceptions);
    }

    private Optional<TodayOccurrenceResult> resolveMovedToTodayOverride(
            Event event,
            LocalDate currentDate,
            List<RecurrenceException> exceptions
    ) {
        return exceptions.stream()
                .filter(ex -> ex.getExceptionType() == OVERRIDE)
                .filter(ex -> ex.getStartTime() != null)
                .filter(ex -> ex.getStartTime().toLocalDate().isEqual(currentDate))
                .findFirst()
                .map(ex -> TodayOccurrenceResult.of(
                        ex.getTitle() != null ? ex.getTitle() : event.getTitle(),
                        ex.getStartTime().toLocalTime(),
                        TargetType.EVENT
                ));
    }

    private TodayOccurrenceResult resolveOccurrenceResult(
            Event event,
            LocalDate currentDate,
            LocalDateTime occurrenceTime,
            List<RecurrenceException> exceptions
    ) {
        Optional<RecurrenceException> occurrenceException = exceptions.stream()
                .filter(ex -> ex.getExceptionDate().isEqual(occurrenceTime))
                .findFirst();

        if (occurrenceException.isEmpty()) {
            return TodayOccurrenceResult.of(
                    event.getTitle(),
                    occurrenceTime.toLocalTime(),
                    TargetType.EVENT
            );
        }

        RecurrenceException exception = occurrenceException.get();

        if (exception.getExceptionType() == SKIP) {
            return TodayOccurrenceResult.none();
        }

        return resolveOverrideResult(event, currentDate, occurrenceTime, exception);
    }

    private TodayOccurrenceResult resolveOverrideResult(
            Event event,
            LocalDate currentDate,
            LocalDateTime occurrenceTime,
            RecurrenceException exception
    ) {
        if (exception.getExceptionType() != OVERRIDE) {
            return TodayOccurrenceResult.none();
        }

        LocalDateTime actualStartTime = exception.getStartTime() != null
                ? exception.getStartTime()
                : LocalDateTime.of(
                exception.getExceptionDate().toLocalDate(),
                event.getStartTime().toLocalTime()
        );

        if (!actualStartTime.toLocalDate().isEqual(currentDate)) {
            return TodayOccurrenceResult.none();
        }

        String resolvedTitle = exception.getTitle() != null
                ? exception.getTitle()
                : event.getTitle();

        return TodayOccurrenceResult.of(
                resolvedTitle,
                actualStartTime.toLocalTime(),
                TargetType.EVENT
        );
    }

    private LocalDateTime findOccurrenceOnDate(Event event, LocalDate targetDate) {
        LocalDateTime currentOccurrenceTime = event.getStartTime();

        if (currentOccurrenceTime.toLocalDate().isEqual(targetDate)) {
            return currentOccurrenceTime;
        }

        RecurrenceGroup recurrenceGroup = event.getRecurrenceGroup();
        Generator generator = generatorFactory.getGenerator(recurrenceGroup);
        EndCondition endCondition = endConditionFactory.getEndCondition(recurrenceGroup);

        int iterationCount = 1;

        while (endCondition.shouldContinue(currentOccurrenceTime, iterationCount, recurrenceGroup)) {
            if (iterationCount >= MAX_OCCURRENCE_ITERATION) {
                break;
            }

            currentOccurrenceTime = generator.next(currentOccurrenceTime, recurrenceGroup);
            iterationCount++;

            if (currentOccurrenceTime.toLocalDate().isAfter(targetDate)) {
                return null;
            }

            if (currentOccurrenceTime.toLocalDate().isEqual(targetDate)) {
                return currentOccurrenceTime;
            }
        }

        return null;
    }

    // 최상위 이벤트 객체를 기준으로 검색 범위에 맞게 임시 시간 Detail DTO를 생성하여 리스트로 반환
    private List<EventResDTO.DetailRes> expandEvents(
            List<Event> baseEvents,
            LocalDateTime startRange,
            LocalDateTime endRange
    ) {

        List<EventResDTO.DetailRes> expandedEvents = new ArrayList<>();

        for (Event event : baseEvents) {
            // 반복 패턴에 맞는 생성기 전략 주입
            Generator generator = generatorFactory.getGenerator(event.getRecurrenceGroup());
            // 반복 패턴에 맞는 정지 조건 전략 주입
            EndCondition endCondition = endConditionFactory.getEndCondition(event.getRecurrenceGroup());

            // 익셉션 테이블 찾기
            List<RecurrenceException> recurrenceExceptions = new ArrayList<>();
            if (event.getRecurrenceGroup() != null) {
                recurrenceExceptions =
                        recurrenceExceptionRepository.findAllByRecurrenceGroupId(event.getRecurrenceGroup().getId());
            }
            LocalDateTime tempStartTime = event.getStartTime();
            LocalDateTime tempEndTime = event.getEndTime();
            RecurrenceException tempEx = null;
            boolean isSkip = false;
            for (RecurrenceException ex : recurrenceExceptions) {
                // 만약 부모 익셉션이 존재한다면
                if (ex.getExceptionDate().isEqual(event.getStartTime())) {
                    log.debug("부모 익셉션 존재");
                    tempEx = ex;
                }
            }
            if (tempEx != null) {
                if (tempEx.getExceptionType() == OVERRIDE) {
                    log.debug("오버라이드 존재");
                    tempStartTime = tempEx.getStartTime() != null ? tempEx.getStartTime() : event.getStartTime();
                    tempEndTime = tempEx.getEndTime() != null ? tempEx.getEndTime() : event.getEndTime();
                }
                else if (tempEx.getExceptionType() == SKIP) {
                    log.debug("스킵 존재");
                    isSkip = true;
                }
                // 부모가 검색 범위에 포함되어 있지 않다면 시간만 추출하고 폐기
                if (!isSkip && !tempStartTime.isBefore(startRange) && !tempEndTime.isAfter(endRange)) {
                    log.debug("예외 부모가 범위에 포함되었습니다");
                    expandedEvents.add(EventConverter.toDetailRes(tempEx, event));
                }
            } else {
                if (!tempStartTime.isBefore(startRange) && !tempEndTime.isAfter(endRange)) {
                    log.debug("원본 부모가 범위에 포함되었습니다");
                    expandedEvents.add(EventConverter.toDetailRes(event));
                }
            }

            // 부모 이벤트 포함
            int count = 1;
            // 생성기에 최초로 들어갈 기준 시간
            LocalDateTime current = event.getStartTime();
            // 끝 시간을 결정하기 위한 범위 계산
            Duration duration = Duration.between(event.getStartTime(), event.getEndTime());

            // endCondition에 의한 무한반복
            while (endCondition.shouldContinue(current, count, event.getRecurrenceGroup())) {

                // 생성기가 패턴을 분석하여 날짜를 생성함
                current = generator.next(current, event.getRecurrenceGroup());
//                log.info("Current occurrenceDate is {}", current);

                // 전략적 생성기가 생성한 임시 객체가 리턴 리스트에 들어갈 자격이 있는가
//                if (endCondition.shouldContinue(current, count, event.getRecurrenceGroup())) {
//                    break;
//                }
                // 익셉션 테이블이 있는 경우
                for (RecurrenceException ex : recurrenceExceptions) {
                    // 타입이 스킵인데, 익셉션 데이트와 요청 시간이 같은 경우
                    if (ex.getExceptionType() == SKIP && current.isEqual(ex.getExceptionDate())) {
                        // 다음 시간으로 넘김
                        current = generator.next(current, event.getRecurrenceGroup());
                    } else if (ex.getExceptionType() == OVERRIDE && current.isEqual(ex.getExceptionDate())) {
                        // 잘 찾았으니 리턴값에 추가
                        expandedEvents.add(EventConverter.toDetailRes(ex, event));
                        current = generator.next(current, event.getRecurrenceGroup());
                    }
                }

                // startRange 이전 → 생성은 하지만 결과에는 미포함
                if (current.plus(duration).isBefore(startRange)) {
                    count++;
                    continue;
                }
                // endRange 초과 → 종료
                if (current.isAfter(endRange)) {
                    break;
                }
                LocalDate endDate = event.getRecurrenceGroup().getEndDate();
                // 패턴의 종료 시간이 정해진 경우에 현재 시간이 종료 시간을 넘어선 경우 → 종료
                if (endDate != null && current.toLocalDate().isAfter(endDate)) {
                    break;
                }
                // 모든 탈출 조건을 통과한 객체는 DTO로 변환
                expandedEvents.add(EventConverter.toDetailRes(event, current, current.plus(duration)));
                // 카운트 증가
                count++;
            }
        }
        return expandedEvents;
    }

    private static List<Event> concatEventList(List<Event> baseEvents, List<Event> EventFromRg) {
        Map<Long, Event> uniqueEvents = new LinkedHashMap<>();

        for (Event e : baseEvents) {
            uniqueEvents.put(e.getId(), e);
        }

        for (Event e : EventFromRg) {
            uniqueEvents.put(e.getId(), e);
        }

        return new ArrayList<>(uniqueEvents.values());
    }

    private NextOccurrenceResult findNextOccurrenceAfter(
            Event event,
            RecurrenceGroup rg,
            LocalDateTime occurrenceTime,
            LocalDateTime now
    ) {
        LocalDateTime currentOccurrenceTime = event.getStartTime();

        Generator generator = generatorFactory.getGenerator(rg);
        EndCondition endCondition = endConditionFactory.getEndCondition(rg);

        int count = 1;

        while (endCondition.shouldContinue(currentOccurrenceTime, count, rg)) {

            // 다음 기본 occurrence 생성
            currentOccurrenceTime = generator.next(currentOccurrenceTime, rg);

            // 해당 날짜의 반복 예외 조회
            Optional<RecurrenceException> exOpt =
                    recurrenceExceptionRepository
                            .findByRecurrenceGroupIdAndExceptionDate(
                                    rg.getId(),
                                    currentOccurrenceTime
                            );

            if (exOpt.isPresent()) {
                RecurrenceException ex = exOpt.get();

                // SKIP, 날짜 수정이 아닌경우
                if (ex.getExceptionType() == SKIP || ex.getStartTime() == null) {
                    count++;
                    continue;
                }
            }

            // 리마인더에 설정된 날짜보다 이후인데, 현재시간보다 이후여야함
            if (currentOccurrenceTime.isAfter(occurrenceTime) && currentOccurrenceTime.isAfter(now)) {
                return NextOccurrenceResult.of(currentOccurrenceTime);
            }

            count++;

            if (count > MAX_OCCURRENCE_ITERATION) {
                break; // 안전장치
            }
        }
        return NextOccurrenceResult.none();
    }

    private LocalDateTime findFirstOccurrenceOnOrAfter(Event event, LocalDateTime now) {
        RecurrenceGroup rg = event.getRecurrenceGroup();

        Generator generator = generatorFactory.getGenerator(rg);
        EndCondition endCondition = endConditionFactory.getEndCondition(rg);

        LocalDateTime current = event.getStartTime();
        LocalDateTime lastValid = null;

        int count = 1;

        // 현재 시간보다 가장 가까운 이후 날짜 찾기
        while (endCondition.shouldContinue(current, count, rg)) {
            current = generator.next(current, rg);
            lastValid = current;
            count++;

            if (!current.isBefore(now)) {
                break;
            }

            if (count > MAX_OCCURRENCE_ITERATION) break;
        }

        return lastValid;
    }
}
