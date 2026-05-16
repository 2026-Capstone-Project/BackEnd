package com.project.backend.domain.chat.converter;

import com.project.backend.domain.chat.dto.response.ChatResDTO;
import com.project.backend.domain.chat.enums.ActionType;
import com.project.backend.domain.chat.enums.ScheduleType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatConverter {

    public static ChatResDTO.SendRes toSendResDTO(
            String reply,
            ActionType action,
            Long scheduleId,
            Long recurrenceGroupId,
            ScheduleType scheduleType
    ) {
        return ChatResDTO.SendRes.builder()
                .reply(reply)
                .action(action)
                .scheduleId(scheduleId)
                .recurrenceGroupId(recurrenceGroupId)
                .scheduleType(scheduleType)
                .build();
    }
}
