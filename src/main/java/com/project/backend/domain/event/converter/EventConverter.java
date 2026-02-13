package com.project.backend.domain.event.converter;

import com.project.backend.domain.event.dto.*;
import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.EventColor;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.reminder.dto.ReminderDeleted;
import com.project.backend.domain.reminder.enums.ChangeType;
import com.project.backend.domain.reminder.enums.DeletedType;
import com.project.backend.domain.reminder.enums.ExceptionChangeType;
import com.project.backend.domain.reminder.enums.TargetType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
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

    public static EventSpec from(EventReqDTO.CreateReq req, LocalDateTime start, LocalDateTime end) {
        return EventSpec.builder()
                .title(req.title())
                .content(req.content())
                .startTime(start)
                .endTime(end)
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

    // RecurrenceException이 없는 일정일경우
    public static EventResDTO.DetailRes toDetailRes(Event event, LocalDateTime occurrenceDate) {
        return EventResDTO.DetailRes.builder()
                .id(event.getId())
                .occurrenceDate(occurrenceDate)
                .calculated(true)
                .title(event.getTitle())
                .content(event.getContent())
                .start(occurrenceDate)
                .end(getEndTime(event, occurrenceDate))
                .location(event.getLocation())
                .isAllDay(event.getIsAllDay())
                .color(event.getColor())
                .recurrenceGroup(RecurrenceGroupConverter.toDetailRes(event.getRecurrenceGroup()))
                .build();
    }

    public static EventResDTO.DetailRes toDetailRes(
            Event event,
            RecurrenceException ex,
            LocalDateTime occurrenceDate) {
        return EventResDTO.DetailRes.builder()
                .id(event.getId())
                .occurrenceDate(ex.getExceptionDate())
                .calculated(true)
                .title(ex.getTitle() != null ? ex.getTitle() : event.getTitle())
                .content(ex.getContent() != null ? ex.getContent() : event.getContent())
                .start(occurrenceDate)
                .end(getEndTime(event, occurrenceDate))
                .location(ex.getLocation() != null ? ex.getLocation() : event.getLocation())
                .isAllDay(ex.getIsAllDay() != null ? ex.getIsAllDay() : event.getIsAllDay())
                .color(ex.getColor() != null ? ex.getColor() : event.getColor())
                .recurrenceGroup(
                        RecurrenceGroupConverter.toDetailRes(ex.getRecurrenceGroup())
                )
                .build();
    }

    // TODO : 오버 로딩 임시조치
    public static EventResDTO.DetailRes toDetailRes(Event event) {
        return EventResDTO.DetailRes.builder()
                .id(event.getId())
                .occurrenceDate(event.getStartTime())
                .calculated(false)
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
                .occurrenceDate(start)
                .calculated(true)
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

    // TODO : 오버 로딩 임시조치
    public static EventResDTO.DetailRes toDetailRes(RecurrenceException ex, Event event) {
        return EventResDTO.DetailRes.builder()
                .id(event.getId())
                .occurrenceDate(ex.getExceptionDate())
                .calculated(true)
                .title(ex.getTitle() != null ? ex.getTitle() : event.getTitle())
                .content(ex.getContent() != null ? ex.getContent() : event.getContent())
                .start(ex.getStartTime() != null ? ex.getStartTime() : ex.getExceptionDate())
                .end(ex.getEndTime() != null ? ex.getEndTime() : getEndTime(event, ex.getExceptionDate()))
                .location(ex.getLocation() != null ? ex.getLocation() : event.getLocation())
                .isAllDay(ex.getIsAllDay() != null ? ex.getIsAllDay() : event.getIsAllDay())
                .color(ex.getColor() != null ? ex.getColor() : event.getColor())
                .recurrenceGroup(
                        RecurrenceGroupConverter.toDetailRes(ex.getRecurrenceGroup())
                )
                .build();
    }

    public static ResolvedOccurrence toResolvedOccurrence(
            Event event,
            RecurrenceException ex,
            LocalDateTime occurrenceDate
    ) {
        return ResolvedOccurrence.builder()
                .occurrenceDate(occurrenceDate)
                .event(event)
                .exception(ex)
                .build();
    }

    public static EventResDTO.EventsListRes toEventsListRes(List<EventResDTO.DetailRes> details) {
        return EventResDTO.EventsListRes.builder()
                .details(details)
                .build();
    }

    public static EventChanged toEventChanged(
            Long targetId,
            Long memberId,
            String title,
            Boolean isRecurring,
            LocalDateTime occurrenceTime,
            ChangeType changeType) {
        return EventChanged.builder()
                .eventId(targetId)
                .memberId(memberId)
                .title(title)
                .isrRecurring(isRecurring)
                .occurrenceTime(occurrenceTime)
                .changeType(changeType)
                .build();
    }

    public static RecurrenceExceptionChanged toRecurrenceExceptionChanged (
            Long exceptionId,
            Long eventId,
            Long memberId,
            String title,
            Boolean isRecurring,
            LocalDateTime occurrenceTime,
            ExceptionChangeType changeType
    ) {
        return RecurrenceExceptionChanged.builder()
                .exceptionId(exceptionId)
                .eventId(eventId)
                .memberId(memberId)
                .title(title)
                .isrRecurring(isRecurring)
                .occurrenceTime(occurrenceTime)
                .changeType(changeType)
                .build();
    }

    public static RecurrenceEnded toEventRecurrenceEnded(Long eventId, LocalDateTime startTime) {
        return RecurrenceEnded.builder()
                .targetId(eventId)
                .targetType(TargetType.EVENT)
                .startTime(startTime)
                .build();
    }

    public static ReminderDeleted toReminderDeleted(
            Long exceptionId,
            Long memberId,
            LocalDateTime occurrenceTime,
            Long targetId,
            TargetType targetType,
            DeletedType deletedType
    ) {
        return ReminderDeleted.builder()
                .exceptionId(exceptionId)
                .memberId(memberId)
                .occurenceTime(occurrenceTime)
                .targetId(targetId)
                .targetType(targetType)
                .deletedType(deletedType)
                .build();
    }

    private static LocalDateTime getEndTime(Event event, LocalDateTime occurrenceDate) {
        LocalDateTime end;
        if (event.getDurationMinutes() != null) {
            end = occurrenceDate.plusMinutes(event.getDurationMinutes());
        } else {
            end = occurrenceDate.plusMinutes(
                    event.getEndTime().toLocalTime().getMinute() - occurrenceDate.toLocalTime().getMinute());
        }
        return end;
    }
}
