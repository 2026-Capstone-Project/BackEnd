package com.project.backend.domain.chat.converter;

import com.project.backend.domain.chat.dto.response.ChatResDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatConverter {

    public static ChatResDTO.SendRes toSendResDTO(String reply) {
        return ChatResDTO.SendRes.builder()
                .reply(reply)
                .build();
    }
}
