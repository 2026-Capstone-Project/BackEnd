package com.project.backend.global.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonPropertyOrder({"isSuccess", "status", "code", "message", "result"})
public class CustomResponse<T> {

    @JsonProperty("isSuccess")
    private boolean isSuccess;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("result")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T result;

    // 기본적으로 200 OK와 특정 message를 전달하는 성공 응답 생성 메서드
    public static <T> CustomResponse<T> onSuccess(String message ,T result) {
        return new CustomResponse<>(true, String.valueOf(HttpStatus.OK.value()), message, result);
    }

    //상태 코드를 받아서 사용하는 성공 응답 생성 메서드
    public static <T> CustomResponse<T> onSuccess(HttpStatus status, String message ,T result) {
        return new CustomResponse<>(true, String.valueOf(status.value()), message, result);
    }

    //실패 응답 생성 메서드 (데이터 포함)
    public static <T> CustomResponse<T> onFailure(String code, String message, T result) {
        return new CustomResponse<>(false, code, message, result);
    }

    //실패 응답 생성 메서드 (데이터 없음)
    public static <T> CustomResponse<T> onFailure(String code, String message) {
        return new CustomResponse<>(false, code, message, null);
    }
}
