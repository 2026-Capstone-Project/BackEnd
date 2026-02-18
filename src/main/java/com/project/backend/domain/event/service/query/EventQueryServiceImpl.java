package com.project.backend.domain.event.service.query;

import com.project.backend.domain.briefing.dto.TodayOccurrenceResult;
import com.project.backend.domain.event.converter.EventConverter;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.ExceptionType;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.factory.EndConditionFactory;
import com.project.backend.domain.event.factory.GeneratorFactory;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.repository.RecurrenceExceptionRepository;
import com.project.backend.domain.event.repository.RecurrenceGroupRepository;
import com.project.backend.domain.event.service.EventOccurrenceResolver;
import com.project.backend.domain.event.strategy.endcondition.EndCondition;
import com.project.backend.domain.event.strategy.generator.Generator;
import com.project.backend.domain.event.validator.EventValidator;
import com.project.backend.domain.reminder.dto.NextOccurrenceResult;
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
import java.util.stream.Collectors;

import static com.project.backend.domain.event.enums.ExceptionType.OVERRIDE;
import static com.project.backend.domain.event.enums.ExceptionType.SKIP;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryServiceImpl implements EventQueryService {

    private final EventRepository eventRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;

    private final GeneratorFactory generatorFactory;
    private final EndConditionFactory endConditionFactory;
    private final EventValidator eventValidator;
    private final EventOccurrenceResolver eventOccurrenceResolver;


    @Override
    public EventResDTO.DetailRes getEventDetail(
            Long eventId,
            LocalDateTime occurrenceDate,
            Long memberId
    ) {
        Event event = eventRepository.findByIdAndMemberId(eventId, memberId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        eventValidator.validateRead(event, occurrenceDate);

        // 찾고자 하는 것이 부모 이벤트인 경우
        if (event.getStartTime().isEqual(occurrenceDate)) {
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

        List<Event> baseEvents = eventRepository.findByMemberIdAndOverlappingRange(memberId, startRange, endRange);
        log.debug("baseEvents = {}", baseEvents);
        List<RecurrenceGroup> baseRg = recurrenceGroupRepository.findActiveRecurrenceGroups(memberId, startDate);
        log.debug("baseRg = {}", baseRg);
        List<Event> EventFromRg = baseRg.stream()
                .map(RecurrenceGroup::getEvent)
                .toList();

        List<Event> result = concatEventList(baseEvents, EventFromRg);

//        // 반복이 있는 일정
//        List<Event> recurringEvent = baseEvents.stream()
//                .filter(e -> e.getRecurrenceGroup() != null)
//                .toList();
//
//        // 반복이 없는 일정
//        List<Event> nonRecurringEvent = baseEvents.stream()
//                .filter(e -> e.getRecurrenceGroup() == null)
//                .toList();

//        List<EventResDTO.DetailRes> eventsListRes = expandEvents(recurringEvent, startRange, endRange);

        // 최상위 이벤트 확장
        List<EventResDTO.DetailRes> eventsListRes = expandEvents(result, startRange, endRange);

        // 시작 날짜 기준으로 정렬
        eventsListRes.sort(
                Comparator.comparing(EventResDTO.DetailRes::start)
        );

        return EventConverter.toEventsListRes(eventsListRes);
    }

    /**
     * 기존에 존재햇던 반복 그룹을 대상으로, 현재 시간보다 이후의 계산된 시간이 있는지 계산
     **/
    @Override
    public NextOccurrenceResult calculateNextOccurrence(Long eventId, LocalDateTime occurrenceTime) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        RecurrenceGroup rg = event.getRecurrenceGroup();

        if (rg == null) {
            return NextOccurrenceResult.none();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastOccurrence = occurrenceTime;
        LocalDateTime current = event.getStartTime();

        Generator generator = generatorFactory.getGenerator(rg);
        EndCondition endCondition = endConditionFactory.getEndCondition(rg);

        int count = 1;

        while (endCondition.shouldContinue(current, count, rg)) {

            // 다음 기본 occurrence 생성
            current = generator.next(current, rg);

            // 해당 날짜의 반복 예외 조회
            Optional<RecurrenceException> exOpt =
                    recurrenceExceptionRepository
                            .findByRecurrenceGroupIdAndExceptionDate(
                                    rg.getId(),
                                    current
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
            if (current.isAfter(lastOccurrence) && current.isAfter(now)) {
                return NextOccurrenceResult.of(current);
            }


            count++;

            if (count > 20_000) {
                break; // 안전장치
            }
        }
        return NextOccurrenceResult.none();
    }


    /**
        * 새로 생성된 반복 그룹을 대상으로, 현재 시간보다 이후의 계산된 시간이 있는지 계산
     **/
    @Override
    public LocalDateTime findNextOccurrenceAfterNow(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        if (event.getRecurrenceGroup() == null) {
            throw new EventException(EventErrorCode.NOT_RECURRING_EVENT);
        }

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

            if (count > 20_000) break;
        }

        return lastValid;
    }

    /**
     * 일정에 대한 반복을 진행해 계산된 일정의 startTime이 오늘인 일정 정보 조회
     **/
    @Override
    public List<TodayOccurrenceResult> calculateTodayOccurrence(List<Long> eventId, LocalDate currentDate) {
        List<TodayOccurrenceResult> result = new ArrayList<>();

        for (Long id : eventId) {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

            // 단일 일정인데
            if (event.getRecurrenceGroup() == null) {
                // startTime이 오늘 날짜와 일치한다면
                if (event.getStartTime().toLocalDate().isEqual(currentDate)) {
                    result.add(TodayOccurrenceResult.of(
                            event.getTitle(),
                            event.getStartTime().toLocalTime(),
                            TargetType.EVENT
                    ));
                } else {
                    result.add(TodayOccurrenceResult.none());
                }
                continue;
            }

            RecurrenceGroup rg = event.getRecurrenceGroup();

            Generator generator = generatorFactory.getGenerator(rg);
            EndCondition endCondition = endConditionFactory.getEndCondition(rg);

            // 예외 날짜 조회
            Set<LocalDateTime> skipDates = recurrenceExceptionRepository
                    .findByRecurrenceGroupId(rg.getId())
                    .stream()
                    .filter(ex -> ex.getExceptionType() == ExceptionType.SKIP)
                    .map(RecurrenceException::getExceptionDate)
                    .collect(Collectors.toSet());

            Optional<RecurrenceException> re = recurrenceExceptionRepository.
                    findByRecurrenceGroupIdAndExceptionDate(
                            rg.getId(),
                            currentDate.atStartOfDay(),
                            currentDate.atTime(23, 59, 59)
                    )
                    .filter(ex -> ex.getExceptionType() == ExceptionType.OVERRIDE);

            // 기준 시간 설정
            LocalTime startTime = event.getStartTime().toLocalTime();
            LocalDateTime current = event.getStartTime();

            String title = event.getTitle();

            if (re.isPresent()) {
                RecurrenceException exception = re.get();
                if (exception.getStartTime() != null) {
                    // 다른 날짜로 수정된 경우
                    if (!exception.getStartTime().toLocalDate().equals(exception.getExceptionDate().toLocalDate())) {
                        result.add(TodayOccurrenceResult.none());
                        continue;
                    }
                    // 시간만 수정된 경우)
                    startTime = exception.getStartTime().toLocalTime();
                }
                title = !Objects.equals(event.getTitle(), exception.getTitle()) ? exception.getTitle() : title;
            }
            // 원본 일정의 startTime이 오늘과 같고
            if (current.toLocalDate().isEqual(currentDate) && !skipDates.contains(current)) {
                result.add(TodayOccurrenceResult.of(title, startTime, TargetType.EVENT));
                continue;
            }

            int count = 1;

            // 현재 시간보다 가장 가까운 이후 날짜 찾기
            while (endCondition.shouldContinue(current, count, rg)) {
                current = generator.next(current, rg);
                count++;

                // 계산된 일정의 날짜가 오늘보다 이후일 경우
                if (current.isAfter(currentDate.atTime(LocalTime.MAX))) {
                    result.add(TodayOccurrenceResult.none());
                    break;
                }

                // 계산된 일정 날짜가 오늘과 일치한다면
                if (current.toLocalDate().isEqual(currentDate)) {
                    result.add(TodayOccurrenceResult.of(title, startTime, TargetType.EVENT));
                    break;
                }

                if (count > 20_000) break;
            }
        }

        return result;
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
            // 부모가 검색 범위에 포함되어 있지 않다면 시간만 추출하고 폐기
            if (!event.getEndTime().isBefore(startRange) && !event.getStartTime().isAfter(endRange)) {
                expandedEvents.add(EventConverter.toDetailRes(event));
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
}
