package com.project.backend.domain.event.validator;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventValidator {

    public void validateCreate(EventReqDTO.CreateReq req) {
        validateTime(req.startTime(), req.endTime());
    }

    public void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new EventException(EventErrorCode.INVALID_TIME);
        }

        if (start.isAfter(end)) {
            throw new EventException(EventErrorCode.INVALID_TIME_RANGE);
        }
    }
}
