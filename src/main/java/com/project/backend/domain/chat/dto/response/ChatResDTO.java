package com.project.backend.domain.chat.dto.response;

import lombok.Builder;

public class ChatResDTO {

    @Builder
    public record SendRes(
            String reply
    ) {
    }
}
