package com.project.backend.domain.briefing.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.backend.domain.briefing.enums.BriefingReason;
import com.project.backend.domain.reminder.enums.TargetType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class BriefingResDTO {

    @Builder
    public record BriefingRes(
            LocalDate date,

            BriefingReason reason,
            List<BriefInfoRes> briefInfo,

            int eventCount,
            int toDoCount
    ) {
    }

    @Builder
    public record BriefInfoRes(
            @JsonFormat(pattern = "HH:mm")
            TargetType targetType,
            LocalTime startTime,
            String title
    ) {
    }
}
