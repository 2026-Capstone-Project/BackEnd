package com.project.backend.domain.event.validator;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
// TODO static으로 바꾸기
@Component
public class EventValidator {

    public void validateCreate(EventReqDTO.CreateReq req) {
        validateTime(req.startTime(), req.endTime());
    }
    
    public void validateRead(Event event, LocalDateTime time) {
        validateMother(event, time);
    }

    public void validateUpdate(
            Event event,
            RecurrenceGroupReqDTO.UpdateReq req,
            LocalDateTime occurrenceDate,
            RecurrenceUpdateScope scope
    ) {
        validateOccurrenceDate(event, occurrenceDate);
        validateScope(event, req, occurrenceDate, scope);
    }

    public void validateDelete(
            Event event,
            LocalDateTime occurrenceDate,
            RecurrenceUpdateScope scope
    ) {
        validateOccurrenceDate(event, occurrenceDate);
        validateScope(event, null, occurrenceDate, scope);
    }

    public void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new EventException(EventErrorCode.INVALID_TIME);
        }

        if (start.isAfter(end)) {
            throw new EventException(EventErrorCode.INVALID_TIME_RANGE);
        }
    }

    private void validateMother(Event event, LocalDateTime originalDate) {
        // 반복이 아닐때
        if (!event.isRecurring()) {
            // 이벤트의 시작시간과 원본 시작시간이 다른경우
            if (!event.getStartTime().isEqual(originalDate)) {
                throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
            }
        }
    }

    private void validateOccurrenceDate(Event event, LocalDateTime occurrenceDate) {
        if (occurrenceDate == null) {
            throw new EventException(EventErrorCode.OCCURRENCE_DATE_REQUIRED);
        }

        // 단일 일정인데, occurrenceDate가 event의 starTime과 일치하지 않을 경우
        if (!event.isRecurring() && !occurrenceDate.isEqual(event.getStartTime())) {
            throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
        }
    }

    private void validateScope(
            Event event,
            RecurrenceGroupReqDTO.UpdateReq req,
            LocalDateTime occurrenceDate,
            RecurrenceUpdateScope scope
    ) {
        boolean isRecurring = event.getRecurrenceGroup() != null;
        // 단일 일정의 원본 삭제인 경우
        if (!isRecurring && req == null && scope != null) {
            throw new EventException(EventErrorCode.UPDATE_SCOPE_NOT_REQUIRED);
        }

        // 반복 일정에 대한 삭제인데 범위 지정 안한 경우
        if (isRecurring && occurrenceDate != null && scope == null) {
            throw new EventException(EventErrorCode.UPDATE_SCOPE_REQUIRED);
        }

        // 반복 일정에 특정 날짜를 기준으로 반복 필드를 수정할 때, THIS_AND_FOLLOWING_EVENTS가 아닌경우
        if (req != null && scope != RecurrenceUpdateScope.THIS_AND_FOLLOWING_EVENTS) {
            throw new EventException(EventErrorCode.THIS_AND_FOLLOWING_EVENTS_ONLY);
        }
    }

}
