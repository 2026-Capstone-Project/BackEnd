package com.project.backend.domain.event.converter;

import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.EventColor;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.member.entity.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventConverter {

    public static Event toEvent(EventReqDTO.CreateReq req, Member member, RecurrenceGroup recurrenceGroup) {
        EventColor color = req.color() != null ? req.color() : EventColor.BLUE;
        Boolean isAllDay = req.isAllDay() != null ? req.isAllDay() : false;
        RecurrenceFrequency rG = recurrenceGroup != null ? recurrenceGroup.getFrequency() : RecurrenceFrequency.NONE;

        return Event.builder()
                .title(req.title())
                .content(req.content())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .location(req.location())
                .recurrenceFrequency(rG)
                .color(color)
                .isAllDay(isAllDay)
                .durationMinutes(null)
                .member(member)
                .recurrenceGroup(recurrenceGroup)
                .build();
    }

    public static EventResDTO.CreateRes toCreateRes(Event event) {
        return EventResDTO.CreateRes.builder()
                .id(event.getId())
                .createdAt(event.getCreatedAt())
                .build();
    }

    // TODO : 오버 로딩 임시조치
    public static EventResDTO.DetailRes toDetailRes(Event event) {
        return EventResDTO.DetailRes.builder()
                .id(event.getId())
                .title(event.getTitle())
                .content(event.getContent())
                .start(event.getStartTime())
                .end(event.getEndTime())
                .location(event.getLocation())
                .isAllDay(event.getIsAllDay())
                .color(event.getColor())
                // TODO : 임시 조치이므로 리팩토링 대상
                .recurrenceGroup(event.getRecurrenceGroup() != null
                        ? RecurrenceGroupConverter.toDetailRes(event.getRecurrenceGroup())
                        : null)
                .build();
    }

    // TODO : 오버 로딩 임시조치
    public static EventResDTO.DetailRes toDetailRes(Event event, LocalDateTime start, LocalDateTime end) {
        return EventResDTO.DetailRes.builder()
                .id(event.getId())
                .title(event.getTitle())
                .content(event.getContent())
                .start(start)
                .end(end)
                .location(event.getLocation())
                .isAllDay(event.getIsAllDay())
                .color(event.getColor())
                .recurrenceGroup(RecurrenceGroupConverter.toDetailRes(event.getRecurrenceGroup()))
                .build();
    }

    public static EventResDTO.EventsListRes toEventsListRes(List<EventResDTO.DetailRes> details) {
        return EventResDTO.EventsListRes.builder()
                .details(details)
                .build();
    }



}
