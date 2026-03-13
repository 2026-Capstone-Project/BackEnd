package com.project.backend.domain.chat.dto.response;

import lombok.Builder;

public class ChatResDTO {

    @Builder
    public record SendRes(
            String reply,
            String conversationId // 히스토리 관리에 활용하는 ID
    ) {
    }
}
