package com.project.backend.domain.event.exception;

import com.project.backend.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum EventErrorCode implements BaseErrorCode {

    INVALID_TIME(HttpStatus.BAD_REQUEST, "EVENT400_1", "시간을 설정하지 않았습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "EVENT400_2", "시간 설정이 잘못되었습니다."),
    INVALID_TITLE(HttpStatus.BAD_REQUEST, "EVENT400_3", "제목에는 공백만 입력할 수 없습니다."),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "EVENT400_4", "위치에는 공백만 입력할 수 없습니다."),
    INVALID_ADDRESS(HttpStatus.BAD_REQUEST, "EVENT400_5", "주소에는 공백만 입력할 수 없습니다."),
    DAYS_OF_WEEK_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT404_4", "WEEKLY 반복의 요일 정보를 찾을 수 없습니다"),
    INVALID_MONTHLY_TYPE
            (HttpStatus.INTERNAL_SERVER_ERROR, "EVENT500_5", "MONTHLY_TYPE이 존재하지 않습니다"),
    INVALID_RECURRENCE_FREQUENCY
            (HttpStatus.INTERNAL_SERVER_ERROR, "EVENT500_6", "FREQUENCY가 존재하지 않습니다"),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT404_1", "일정을 찾을 수 없습니다"),
    UPDATE_SCOPE_NOT_REQUIRED(HttpStatus.BAD_REQUEST, "EVENT400_3", "UPDATE_SCOPE 설정이 필요하지 않습니다."),
    OCCURRENCE_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "EVENT400_5", "OCCURRENCE_DATE가 없습니다."),
    UPDATE_SCOPE_REQUIRED(HttpStatus.BAD_REQUEST, "EVENT400_7", "UPDATE_SCOPE가 없습니다."),
    INVALID_UPDATE_SCOPE(HttpStatus.BAD_REQUEST, "EVENT400_8", "존재하지 않는UPDATE_SCOPE 값입니다."),
    NOT_RECURRING_EVENT(HttpStatus.BAD_REQUEST, "EVENT400_9", "단일 일정입니다."),
    THIS_AND_FOLLOWING_EVENTS_ONLY
            (HttpStatus.BAD_REQUEST, "EVENT400_10",
                    "반복 필드 수정, 단일 일정 -> 반복 일정으로 수정 시, THIS_AND_FOLLOWING_EVENTS만 가능합니다."),
    EVENT_INVITEE_NOT_FRIEND(HttpStatus.NOT_FOUND, "EVENT404_5", "주최자와 친구사이가 아닙니다."),
    EVENT_SELF_INVITE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "EVENT400_11", "자기자신은 초대할 수 없습니다."),
    INVALID_PARTICIPANT_UPDATE_SCOPE(HttpStatus.BAD_REQUEST, "EVENT400_12", "ThisEvent일 때는 공유 불가."),

    EVENT_INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT404_6", "해당 일정 공유 초대가 존재하지 않습니다."),
    EVENT_INVITATION_FORBIDDEN(HttpStatus.FORBIDDEN, "EVENT403_1", "해당 일정 공유 초대에 대한 권한이 없습니다"),

    EVENT_OWNER_CANNOT_LEAVE(HttpStatus.FORBIDDEN, "EVENT403_2", "이벤트 소유자는 이벤트를 떠날 수 없습니다"),
    ;



    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
