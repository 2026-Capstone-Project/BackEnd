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

import java.time.Duration;
import java.time.LocalDateTime;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventConverter {

    public static Event toEvent(EventSpec spec, Member member, RecurrenceGroup recurrenceGroup) {
        EventColor color = spec.color() != null ? spec.color() : EventColor.BLUE;
        Boolean isAllDay = spec.isAllDay() != null ? spec.isAllDay() : false;
        RecurrenceFrequency rG = recurrenceGroup != null ? recurrenceGroup.getFrequency() : RecurrenceFrequency.NONE;
        Integer durationMinutes =
                (spec.startTime() != null && spec.endTime() != null)
                        ? (int) Duration.between(spec.startTime(), spec.endTime()).toMinutes()
                        : null;

        return Event.builder()
                .title(spec.title())
                .content(spec.content())
                .startTime(spec.startTime())
                .endTime(spec.endTime())
                .location(spec.location())
                .recurrenceFrequency(rG)
                .color(color)
                .isAllDay(isAllDay)
                .durationMinutes(durationMinutes)
                .member(member)
                .recurrenceGroup(recurrenceGroup)
                .build();
    }

    public static EventSpec from(EventReqDTO.CreateReq req) {
        return EventSpec.builder()
                .title(req.title())
                .content(req.content())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .location(req.location())
                .color(req.color())
                .isAllDay(req.isAllDay())
                .build();
    }

    public static EventSpec from(EventReqDTO.UpdateReq req, Event event, LocalDateTime start, LocalDateTime end) {
        return EventSpec.builder()
                .title(req.title() != null ? req.title() : event.getTitle())
                .content(req.content() != null ? req.content() : event.getContent())
                .startTime(start)
                .endTime(end)
                .location(req.location() != null ? req.location() : event.getLocation())
                .color(req.color() != null ? req.color() : event.getColor())
                .isAllDay(req.isAllDay() != null ? req.isAllDay() : event.getIsAllDay())
                .build();
    }

    public static EventResDTO.CreateRes toCreateRes(Event event) {
        return EventResDTO.CreateRes.builder()
                .id(event.getId())
                .createdAt(event.getCreatedAt())
                .build();
    }

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
                .recurrenceGroup(RecurrenceGroupConverter.toDetailRes(event.getRecurrenceGroup()))
                .build();
    }
}
