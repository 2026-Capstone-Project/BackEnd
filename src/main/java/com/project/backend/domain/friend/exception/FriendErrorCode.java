package com.project.backend.domain.friend.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum FriendErrorCode implements BaseErrorCode {

    FRIEND_REQUEST_FORBIDDEN(HttpStatus.FORBIDDEN, "FRIEND403_1", "해당 친구 요청에 대한 접근 권한이 없습니다"),
    FRIEND_FORBIDDEN(HttpStatus.FORBIDDEN, "FRIEND403_2", "해당 친구에 대한 접근 권한이 없습니다"),

    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND404_1", "해당 친구 요청을 찾을 수 없습니다"),
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "FRIEND404_2", "해당 친구를 찾을 수 없습니다"),

    ALREADY_FRIEND(HttpStatus.CONFLICT, "FRIEND409_1", "이미 친구입니다."),
    ALREADY_REQUESTED(HttpStatus.CONFLICT, "FRIEND409_2", "이미 친구 요청이 전송되었습니다"),
    FRIEND_SAVE_CONFLICT(HttpStatus.CONFLICT, "FRIEND409_3", "친구 저장 충돌"),
    FRIEND_REQUEST_SAVE_CONFLICT(HttpStatus.CONFLICT, "FRIEND409_4", "친구 요청 저장 충돌"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
