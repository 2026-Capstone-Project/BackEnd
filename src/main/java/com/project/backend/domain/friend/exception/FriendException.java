package com.project.backend.domain.friend.exception;

import com.project.backend.global.apiPayload.exception.CustomException;

public class FriendException extends CustomException {
    public FriendException(FriendErrorCode errorCode) {
        super(errorCode);
    }
}
