package com.project.backend.domain.event.validator;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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

    public void validateUpdate(EventReqDTO.UpdateReq req, Event event) {
        validateOccurrenceDate(req.occurrenceDate());
        validateScope(event, req.occurrenceDate(), req.recurrenceUpdateScope());
    }

    public void validateDelete(
            Event event,
            LocalDate occurrenceDate,
            RecurrenceUpdateScope scope
    ) {
        validateOccurrenceDate(occurrenceDate);
        validateScope(event, occurrenceDate, scope);
    }

    public void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new EventException(EventErrorCode.INVALID_TIME);
        }

        if (start.isAfter(end)) {
            throw new EventException(EventErrorCode.INVALID_TIME_RANGE);
        }
    }

    private void validateMother(Event event, LocalDateTime time) {
        if (!event.isRecurring() && !event.getStartTime().isEqual(time)) {
            throw new EventException(EventErrorCode.EVENT_NOT_FOUND);
        }
    }

    private void validateOccurrenceDate(LocalDate occurrenceDate) {
        if (occurrenceDate == null) {
            throw new EventException(EventErrorCode.OCCURRENCE_DATE_REQUIRED);
        }
    }

    private void validateScope(
            Event event,
            LocalDate occurrenceDate,
            RecurrenceUpdateScope scope
    ) {
        boolean isRecurring = event.getRecurrenceGroup() != null;

        // 단일 일정의 원본 삭제인 경우
        if (!isRecurring && scope != null) {
            throw new EventException(EventErrorCode.UPDATE_SCOPE_NOT_REQUIRED);
        }

        // 반복 일정에 대한 삭제인데 범위 지정 안한 경우
        if (isRecurring && occurrenceDate != null && scope == null) {
            throw new EventException(EventErrorCode.UPDATE_SCOPE_REQUIRED);
        }
    }

}
