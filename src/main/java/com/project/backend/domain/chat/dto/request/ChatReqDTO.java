package com.project.backend.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ChatReqDTO {

    public record SendReq(
            @NotBlank(message = "메시지는 필수 요소입니다.")
            String message
    ) {
    }
}
