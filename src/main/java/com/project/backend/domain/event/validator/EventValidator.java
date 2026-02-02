package com.project.backend.domain.event.validator;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class EventValidator {

    public void validateCreate(EventReqDTO.CreateReq req) {
        validateTime(req.startTime(), req.endTime());
    }

    public void validateUpdate(EventReqDTO.UpdateReq req, Event event) {
        validateOccurrenceDate(req, event);
        validateUpdateType(req, event.getRecurrenceGroup());
    }

    public void validateDelete(Event event, LocalDate occurrenceDate, RecurrenceUpdateScope scope) {
        validateOccurrenceDateForDelete(event, occurrenceDate, scope);
    }

    public void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new EventException(EventErrorCode.INVALID_TIME);
        }

        if (start.isAfter(end)) {
            throw new EventException(EventErrorCode.INVALID_TIME_RANGE);
        }
    }

    public void validateUpdateType(EventReqDTO.UpdateReq req, RecurrenceGroup rg) {
        RecurrenceUpdateScope scope = req.recurrenceUpdateScope();

        boolean wasRecurring = (rg != null);
        boolean wantsRecurrenceChange = (req.recurrenceGroup() != null);

        // 원래 반복 → 반복 규칙 수정 - scope 필수
        if (wasRecurring && wantsRecurrenceChange && scope == null) {
            throw new EventException(EventErrorCode.UPDATE_SCOPE_REQUIRED);
        }

        // 원래 단일 → scope만 보냄 (반복도 없는데 범위 지정)
        if (!wasRecurring && !wantsRecurrenceChange && scope != null) {
            throw new EventException(EventErrorCode.UPDATE_SCOPE_NOT_REQUIRED);
        }
    }
    public void validateOccurrenceDate(
            EventReqDTO.UpdateReq req,
            Event event
    ) {
        RecurrenceGroup rg = event.getRecurrenceGroup();

        // 반복이 아닌 이벤트는 occurrenceDate 자체가 의미 없음
        if (rg == null && req.occurrenceDate() != null) {
            throw new EventException(EventErrorCode.INVALID_OCCURRENCE_DATE);
        }

        // 반복이 있는데 occurrenceDate가 없는 경우
        if (rg != null && req.occurrenceDate() == null) {
            // 계산되지 않은 실제 일정일 경우(DB에 저장된 원본 일정) 변경시 RecurrenceUpdateScope은 모든 이벤트에 적용만 가능하다
            // 때문에 모든 이벤트를 변경하는 경우가 아니라면 이 요청은 반복은 존재하는데 occurrenceDate가 없는 잘못된 요청이다.
            if (req.recurrenceUpdateScope() != RecurrenceUpdateScope.ALL_EVENTS)
                throw new EventException(EventErrorCode.ONLY_ALL_EVENTS_ALLOWED_FOR_ORIGINAL_EVENT);
        }

        // occurrenceDate가 해당 일정의 반복 그룹 계산을 통해 도출된 날짜가 맞는지
//        boolean isValid = recurrenceCalculator
//                .isOccurrenceDate(rg, event.getStartTime(), occurrenceDate);
//
//        if (!isValid) {
//            throw new EventException(EventErrorCode.INVALID_OCCURRENCE_DATE);
//        }
    }

    private void validateOccurrenceDateForDelete(
            Event event,
            LocalDate occurrenceDate,
            RecurrenceUpdateScope scope
    ) {
        // 1. 실제(엄마) 일정인데 ALL_EVENTS가 아닌 경우
        if (occurrenceDate == null
                && event.getRecurrenceGroup() != null
                && scope != RecurrenceUpdateScope.ALL_EVENTS) {
            throw new EventException(
                    EventErrorCode.ONLY_ALL_EVENTS_ALLOWED_FOR_ORIGINAL_EVENT
            );
        }

        // 2. 계산된 일정인데 scope가 없는 경우
        if (occurrenceDate != null && scope == null) {
            throw new EventException(EventErrorCode.UPDATE_SCOPE_REQUIRED);
        }

        // 3. 반복 없는 일정인데 scope가 있는 경우
        if (event.getRecurrenceGroup() == null && scope != null) {
            throw new EventException(EventErrorCode.UPDATE_SCOPE_NOT_REQUIRED);
        }
    }
}
