package com.project.backend.domain.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.backend.domain.notification.enums.BriefingReason;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class NotificationResDTO {

    @Builder
    public record BriefingRes(
            LocalDate date,

            BriefingReason reason,
            List<BriefInfoRes> briefInfo,

            int eventCount,
            int toDoCount
    ) {
        public static BriefingRes unavailable(LocalDate date) {
            return new BriefingRes(date, BriefingReason.DISABLED, null,0, 0);
        }

        public static BriefingRes empty(LocalDate date) {
            return new BriefingRes(date, BriefingReason.NOT_EVENT_TODAY, null,0, 0);
        }

        public static BriefingRes available(
                LocalDate date,
                List<BriefInfoRes> briefInfo,
                int eventCount,
                int toDoCount
        ) {
            return new BriefingRes(date, BriefingReason.AVAILABLE, briefInfo, eventCount, toDoCount);
        }
    }

    @Builder
    public record BriefInfoRes(
            @JsonFormat(pattern = "HH:mm")
            LocalTime startTime,
            String title
    ) {
    }
}
