package com.project.backend.domain.notification.converter;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.notification.dto.response.NotificationResDTO;
import com.project.backend.domain.notification.enums.BriefingReason;
import com.project.backend.domain.todo.entity.Todo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationConverter {

    public static NotificationResDTO.BriefingRes toBriefingRes (
            LocalDate now,
            BriefingReason reason,
            List<NotificationResDTO.BriefInfoRes> infoRes,
            int eventCount,
            int toDoCount
    ) {
        return NotificationResDTO.BriefingRes.builder()
                .date(now)
                .reason(reason)
                .briefInfo(infoRes)
                .eventCount(eventCount)
                .toDoCount(toDoCount)
                .build();
    }

    public static NotificationResDTO.BriefInfoRes toBriefEventInfoRes (Event event) {
        return NotificationResDTO.BriefInfoRes.builder()
                .title(event.getTitle())
                .startTime(event.getStartTime().toLocalTime())
                .build();
    }

//    public static NotificationResDTO.BriefInfoRes toBriefTodoInfoRes (Todo todo) {
//        return NotificationResDTO.BriefInfoRes.builder()
//                .title(todo.getTitle())
//                .startTime(todo.getStartTime().toLocalTime())
//                .build();
//    }

}
