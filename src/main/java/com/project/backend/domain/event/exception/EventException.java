package com.project.backend.domain.event.exception;

import com.project.backend.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class EventException extends CustomException {

    public EventException(EventErrorCode errorCode) {
        super(errorCode);
    }
}
