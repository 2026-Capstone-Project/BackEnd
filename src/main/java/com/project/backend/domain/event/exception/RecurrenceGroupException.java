package com.project.backend.domain.event.exception;

import com.project.backend.global.apiPayload.exception.CustomException;
import lombok.Getter;

@Getter
public class RecurrenceGroupException extends CustomException {

    public RecurrenceGroupException(RecurrenceGroupErrorCode errorCode) {
        super(errorCode);
    }
}
