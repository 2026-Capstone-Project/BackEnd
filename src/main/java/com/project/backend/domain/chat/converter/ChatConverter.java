package com.project.backend.domain.chat.converter;

import com.project.backend.domain.chat.dto.response.ChatResDTO;
import com.project.backend.domain.chat.enums.ActionType;
import com.project.backend.domain.chat.enums.ScheduleType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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

    public static ChatResDTO.HistoryRes toHistoryResDTO(List<Map<String, String>> messages, String summary) {
        List<ChatResDTO.MessageRes> messageResList = messages.stream()
                .map(m -> new ChatResDTO.MessageRes(m.get("role"), m.get("content")))
                .toList();

        return ChatResDTO.HistoryRes.builder()
                .messages(messageResList)
                .summary(summary)
                .build();
    }
}
