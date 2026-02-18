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

import static com.project.backend.domain.event.enums.ExceptionType.OVERRIDE;
import static com.project.backend.domain.event.enums.ExceptionType.SKIP;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventOccurrenceResolver {

    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final GeneratorFactory generatorFactory;
    private final EndConditionFactory endConditionFactory;

    public EventResDTO.DetailRes resolveForRead(Event event, LocalDateTime occurrenceDate) {
        ResolvedOccurrence ro = resolveInternal(event, occurrenceDate);

        if (ro.exception() != null) {
            return EventConverter.toDetailRes(ro.event(), ro.exception(), ro.occurrenceDate());
        } else {
            return EventConverter.toDetailRes(ro.event(), ro.occurrenceDate());
        }
    }

    public void assertOccurrenceExists(Event event, LocalDateTime occurrenceDate) {
        resolveInternal(event, occurrenceDate);
    }

    private ResolvedOccurrence resolveInternal(Event event, LocalDateTime occurrenceDate) {
        // 반복 + 일정이 아닌경우
        if (!event.isRecurring()) {
            throw new EventException(EventErrorCode.NOT_RECURRING_EVENT);
        }

        // occurrenceDate가 수정/삭제된 일정의 태생적 날짜+시간 인지
        Optional<RecurrenceException> exception = recurrenceExceptionRepository.
                findByRecurrenceGroupIdAndExceptionDate(event.getRecurrenceGroup().getId(), occurrenceDate);

        if (exception.isPresent()) {
            RecurrenceException ex = exception.get();
            // 타입이 오버라이드(수정)
            if (ex.getExceptionType() == OVERRIDE) {
                // 날짜는 수정되지 않은 경우
                if (ex.getStartTime() == null) {
                    return EventConverter.toResolvedOccurrence(event, ex, occurrenceDate);
                }
                // 날짜가 수정된 경우
                return EventConverter.toResolvedOccurrence(event, ex, ex.getStartTime());
            }
            // 타입이 스킵이면 보여주면 안되는 객체
            if (ex.getExceptionType() == SKIP) {
                throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
            }
        }

        // 생성기에 최초로 들어갈 기준 시간
        LocalDateTime current = event.getStartTime();

        if (current.isEqual(occurrenceDate)) {
            return EventConverter.toResolvedOccurrence(event, null, current);
        }

        // 생성기 & 종료 조건 생성
        Generator generator = generatorFactory.getGenerator(event.getRecurrenceGroup());
        EndCondition endCondition = endConditionFactory.getEndCondition(event.getRecurrenceGroup());

        int count = 1;

        // endCondition에 의한 무한반복
        while (endCondition.shouldContinue(current, count, event.getRecurrenceGroup())) {

            // 시간 생성
            current = generator.next(current, event.getRecurrenceGroup());

            // 이벤트를 찾은 경우
            if (current.equals(occurrenceDate)) {
                return EventConverter.toResolvedOccurrence(event, null, current);
            }
            // 검색하고자 했던 시간을 넘어선 경우
            if (current.isAfter(occurrenceDate)) {
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
}
