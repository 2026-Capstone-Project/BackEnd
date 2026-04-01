package com.project.backend.domain.chat.dto.response;

import com.project.backend.domain.chat.enums.ActionType;
import com.project.backend.domain.chat.enums.ScheduleType;
import lombok.Builder;

public class ChatResDTO {

    @Builder
    public record SendRes(
            String reply,
            ActionType action,          // CREATED / UPDATED / DELETED / CLARIFYING / NONE
            Long scheduleId,            // 단건 갱신용, action이 NONE/CLARIFYING이면 null
            Long recurrenceGroupId,     // 반복 그룹 전체 갱신용, 반복 일정이 아니면 null
            ScheduleType scheduleType   // EVENT / TODO, action이 NONE/CLARIFYING이면 null
    ) {}
}
