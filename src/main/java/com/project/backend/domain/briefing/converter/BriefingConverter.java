package com.project.backend.domain.briefing.converter;

import com.project.backend.domain.briefing.dto.TodayOccurrenceResult;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.briefing.dto.response.BriefingResDTO;
import com.project.backend.domain.briefing.enums.BriefingReason;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BriefingConverter {

    public static BriefingResDTO.BriefingRes toBriefingRes (
            LocalDate now,
            BriefingReason reason,
            List<BriefingResDTO.BriefInfoRes> infoRes,
            int eventCount,
            int toDoCount
    ) {
        return BriefingResDTO.BriefingRes.builder()
                .date(now)
                .reason(reason)
                .briefInfo(infoRes)
                .eventCount(eventCount)
                .toDoCount(toDoCount)
                .build();
    }

    public static BriefingResDTO.BriefInfoRes toBriefInfoRes (TodayOccurrenceResult result) {
        return BriefingResDTO.BriefInfoRes.builder()
                .targetType(result.targetType())
                .title(result.title())
                .startTime(result.time())
                .build();
    }

    public static BriefingResDTO.BriefingRes toUnavailable(LocalDate date) {
        return BriefingResDTO.BriefingRes.builder()
                .date(date)
                .reason(BriefingReason.DISABLED)
                .briefInfo(null)
                .eventCount(0)
                .toDoCount(0)
                .build();
    }

    public static BriefingResDTO.BriefingRes toEmpty(LocalDate date) {
        return BriefingResDTO.BriefingRes.builder()
                .date(date)
                .reason(BriefingReason.NOT_EVENT_TODAY)
                .briefInfo(null)
                .eventCount(0)
                .toDoCount(0)
                .build();
    }

    public static BriefingResDTO.BriefingRes toAvailable(
            LocalDate date,
            List<BriefingResDTO.BriefInfoRes> briefInfo,
            int eventCount,
            int toDoCount
    ) {
        return BriefingResDTO.BriefingRes.builder()
                .date(date)
                .reason(BriefingReason.AVAILABLE)
                .briefInfo(briefInfo)
                .eventCount(eventCount)
                .toDoCount(toDoCount)
                .build();
    }


//    public static NotificationResDTO.BriefInfoRes toBriefTodoInfoRes (Todo todo) {
//        return NotificationResDTO.BriefInfoRes.builder()
//                .title(todo.getTitle())
//                .startTime(todo.getStartTime().toLocalTime())
//                .build();
//    }

}
