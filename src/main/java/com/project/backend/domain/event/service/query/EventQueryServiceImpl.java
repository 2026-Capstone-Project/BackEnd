package com.project.backend.domain.event.service.query;

import com.project.backend.domain.event.converter.EventConverter;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.factory.EndConditionFactory;
import com.project.backend.domain.event.factory.GeneratorFactory;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.repository.RecurrenceExceptionRepository;
import com.project.backend.domain.event.repository.RecurrenceGroupRepository;
import com.project.backend.domain.event.strategy.endcondition.EndCondition;
import com.project.backend.domain.event.strategy.generator.Generator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.project.backend.domain.event.enums.ExceptionType.OVERRIDE;
import static com.project.backend.domain.event.enums.ExceptionType.SKIP;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryServiceImpl implements EventQueryService {

    private final EventRepository eventRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;

    private final GeneratorFactory generatorFactory;
    private final EndConditionFactory endConditionFactory;
    private final RecurrenceGroupRepository recurrenceGroupRepository;


    @Override
    public EventResDTO.DetailRes getEventDetail(Long eventId, LocalDateTime time, Long memberId) {

        Event event = eventRepository.findByMemberIdAndId(memberId, eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        // 익셉션 테이블 생성
        List<RecurrenceException> recurrenceExceptions =
                recurrenceExceptionRepository.findByRecurrenceGroupId(event.getRecurrenceGroup().getId());
        log.info(recurrenceExceptions.toString());

        // 테이블이 있는 경우
        for (RecurrenceException ex : recurrenceExceptions) {
            // 타입이 스킵인데, 익셉션 데이트와 요청 시간이 같은 경우
            if (ex.getExceptionType() == SKIP && time.toLocalDate().isEqual(ex.getExceptionDate())) {
                // 보여주면 안되는 객체
                throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
            // 타입이 오버라이드인데, 시간이 변경 시간과 같은 경우
            } else if (ex.getExceptionType() == OVERRIDE && time.isEqual(ex.getStartTime())) {
                // 잘 찾았으니 리턴
                return EventConverter.toDetailRes(ex, event);
            }
        }

        // 찾고자 하는 것이 부모 이벤트인 경우
        if (event.getStartTime().isEqual(time)) {
            log.debug("부모 이벤트 발견");
            return EventConverter.toDetailRes(event);
        }

        // 생성기에 최초로 들어갈 기준 시간
        LocalDateTime current = event.getStartTime();

        // 생성기 & 종료 조건 생성
        Generator generator = generatorFactory.getGenerator(event.getRecurrenceGroup());
        EndCondition endCondition = endConditionFactory.getEndCondition(event.getRecurrenceGroup());

        int count = 1;

        // endCondition에 의한 무한반복
        while (endCondition.shouldContinue(current, count, event.getRecurrenceGroup())) {

            // 시간 생성
            current = generator.next(current, event.getRecurrenceGroup());

            // 이벤트를 찾은 경우
            if (current.equals(time)) {
                return EventConverter.toDetailRes(event, current);
            }
            // 검색하고자 했던 시간을 넘어선 경우
            if (current.isAfter(time)) {
                log.debug("설정한 이벤트 종료 시간 초과");
                break;
            }
            // 모든 탈출 조건문에 걸리지 않았을 때, 최후의 종료
            if (count > 20000) {
                log.debug("반복 한도 초과");
                break;
            }
            count++;
        }
        // 아무것도 찾지 못하고 반복 종료 시
        throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
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

        return EventConverter.toEventsListRes(eventsListRes);
    }


    // 최상위 이벤트 객체를 기준으로 검색 범위에 맞게 임시 시간 Detail DTO를 생성하여 리스트로 반환
    private List<EventResDTO.DetailRes> expandEvents(List<Event> baseEvents, LocalDateTime startRange, LocalDateTime endRange) {

        List<EventResDTO.DetailRes> expandedEvents = new ArrayList<>();

        for (Event event : baseEvents) {
            // 반복 패턴에 맞는 생성기 전략 주입
            Generator generator = generatorFactory.getGenerator(event.getRecurrenceGroup());
            // 반복 패턴에 맞는 정지 조건 전략 주입
            EndCondition endCondition = endConditionFactory.getEndCondition(event.getRecurrenceGroup());

            // 익셉션 테이블 찾기
            List<RecurrenceException> recurrenceExceptions =
                    recurrenceExceptionRepository.findByRecurrenceGroupId(event.getRecurrenceGroup().getId());

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
//                log.info("Current time is {}", current);

                // 전략적 생성기가 생성한 임시 객체가 리턴 리스트에 들어갈 자격이 있는가
//                if (endCondition.shouldContinue(current, count, event.getRecurrenceGroup())) {
//                    break;
//                }
                // 익셉션 테이블이 있는 경우
                for (RecurrenceException ex : recurrenceExceptions) {
                    // 타입이 스킵인데, 익셉션 데이트와 요청 시간이 같은 경우
                    if (ex.getExceptionType() == SKIP && current.toLocalDate().isEqual(ex.getExceptionDate())) {
                        // 다음 시간으로 넘김
                        current = generator.next(current, event.getRecurrenceGroup());
                    } else if (ex.getExceptionType() == OVERRIDE && current.toLocalDate().isEqual(ex.getExceptionDate())) {
                        // 잘 찾았으니 리턴값에 추가
                        expandedEvents.add(EventConverter.toDetailRes(ex, event));
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
