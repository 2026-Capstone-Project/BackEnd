package com.project.backend.domain.event.service;

import com.project.backend.domain.event.converter.EventConverter;
import com.project.backend.domain.event.dto.ResolvedOccurrence;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.factory.EndConditionFactory;
import com.project.backend.domain.event.factory.GeneratorFactory;
import com.project.backend.domain.event.repository.RecurrenceExceptionRepository;
import com.project.backend.domain.event.strategy.endcondition.EndCondition;
import com.project.backend.domain.event.strategy.generator.Generator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.project.backend.domain.common.recurrence.enums.ExceptionType.OVERRIDE;
import static com.project.backend.domain.common.recurrence.enums.ExceptionType.SKIP;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventOccurrenceResolver {

    private static final int MAX_OCCURRENCE_ITERATION = 20_000;

    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final GeneratorFactory generatorFactory;
    private final EndConditionFactory endConditionFactory;

    public EventResDTO.DetailRes resolveForRead(Event event, LocalDateTime occurrenceDate) {
        ResolvedOccurrence ro = resolveInternal(event, occurrenceDate);

        if (ro.exception() != null) {
            return EventConverter.toDetailRes(ro.event(), ro.exception(), ro.occurrenceDate());
        }

        return EventConverter.toDetailRes(ro.event(), ro.occurrenceDate());
    }

    public void assertOccurrenceExists(Event event, LocalDateTime occurrenceDate) {
        resolveInternal(event, occurrenceDate);
    }

    private ResolvedOccurrence resolveInternal(Event event, LocalDateTime occurrenceDate) {
        // occurrenceDate가 수정/삭제된 일정의 태생적 날짜+시간 인지
        Optional<RecurrenceException> exception = recurrenceExceptionRepository.
                findByRecurrenceGroupIdAndExceptionDate(event.getRecurrenceGroup().getId(), occurrenceDate);

        if (exception.isPresent()) {
            return resolveFromException(event, occurrenceDate, exception.get());
        }

        // 생성기에 최초로 들어갈 기준 시간
        LocalDateTime current = event.getStartTime();

        // 반복 일정에 대한 첫 시작 일정의 startTime과 occurrenceTime이 일치하면 부모 일정의 값 반환해줌
        if (current.isEqual(occurrenceDate)) {
            return EventConverter.toResolvedOccurrence(event, null, current);
        }

        return resolveGeneratedOccurrence(event, occurrenceDate, current);
    }

    private ResolvedOccurrence resolveGeneratedOccurrence(
            Event event, LocalDateTime occurrenceDate, LocalDateTime currentOccurrenceTime) {
        // 생성기 & 종료 조건 생성
        Generator generator = generatorFactory.getGenerator(event.getRecurrenceGroup());
        EndCondition endCondition = endConditionFactory.getEndCondition(event.getRecurrenceGroup());

        int count = 1;

        // endCondition에 의한 무한반복
        while (endCondition.shouldContinue(currentOccurrenceTime, count, event.getRecurrenceGroup())) {
            // 시간 생성
            currentOccurrenceTime = generator.next(currentOccurrenceTime, event.getRecurrenceGroup());

            // 이벤트를 찾은 경우
            if (currentOccurrenceTime.equals(occurrenceDate)) {
                return EventConverter.toResolvedOccurrence(event, null, currentOccurrenceTime);
            }
            // 검색하고자 했던 시간을 넘어선 경우
            if (currentOccurrenceTime.isAfter(occurrenceDate)) {
                log.debug("설정한 이벤트 종료 시간 초과");
                break;
            }
            // 모든 탈출 조건문에 걸리지 않았을 때, 최후의 종료
            if (count > MAX_OCCURRENCE_ITERATION) {
                log.debug("반복 한도 초과");
                break;
            }
            count++;
        }
        // 아무것도 찾지 못하고 반복 종료 시
        throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
    }

    private ResolvedOccurrence resolveFromException(Event event, LocalDateTime occurrenceDate, RecurrenceException ex) {
        // 타입이 스킵이면 보여주면 안되는 객체
        if (ex.getExceptionType() == SKIP) {
            throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
        }

        // 타입이 오버라이드(수정)
        if (ex.getExceptionType() == OVERRIDE) {
            // Exception에 startTime이 있다면 수정된 것이고 아니라면 수정되지 않은 것.
            LocalDateTime resolvedTime = (ex.getStartTime() == null) ? occurrenceDate : ex.getStartTime();
            return EventConverter.toResolvedOccurrence(event, ex, resolvedTime);
        }

        throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
    }
}
