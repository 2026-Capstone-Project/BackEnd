package com.project.backend.domain.event.service.command;

import com.project.backend.domain.event.converter.EventConverter;
import com.project.backend.domain.event.converter.EventSpec;
import com.project.backend.domain.event.converter.RecurrenceGroupConverter;
import com.project.backend.domain.event.converter.RecurrenceGroupSpec;
import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.repository.RecurrenceExceptionRepository;
import com.project.backend.domain.event.repository.RecurrenceGroupRepository;
import com.project.backend.domain.event.validator.EventValidator;
import com.project.backend.domain.event.validator.RecurrenceGroupValidator;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventCommandServiceImpl implements EventCommandService {

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final RecurrenceExceptionRepository recurrenceExRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final EventValidator eventValidator;
    private final RecurrenceGroupValidator rgValidator;

    @Override
    public EventResDTO.CreateRes createEvent(EventReqDTO.CreateReq req, Long memberId) {
        eventValidator.validateCreate(req);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        RecurrenceGroup recurrenceGroup = null;
        RecurrenceGroupSpec rgSpec = null;

        if (req.recurrenceGroup() != null) {

            rgValidator.validateCreate(req.recurrenceGroup(), req.startTime());

            rgSpec = RecurrenceGroupConverter.from(req.recurrenceGroup(), req.startTime());
            recurrenceGroup = createRecurrenceGroup(rgSpec, member);
        }

        // 반복 그룹을 통해 설정한 반복 요일, 일, 월에 따라 일정 startTime, endTime 변경
        AdjustedTime adjustedTime = RecurrenceTimeAdjuster.adjust(req.startTime(), req.endTime(), rgSpec);

        EventSpec eventSpec = EventConverter.from(req, adjustedTime.start(), adjustedTime.end());
        Event event = createEvent(eventSpec, member, recurrenceGroup);

        return EventConverter.toCreateRes(event);
    }

    @Override
    public void updateEvent(EventReqDTO.UpdateReq req, Long eventId, Long memberId) {
        Event event = eventRepository.findByMemberIdAndId(memberId, eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        eventValidator.validateUpdate(req, event);

        // 수정안한 계산된 일정의 날짜인지, 수정된 날짜인지 계산
        LocalDateTime start = calStartTime(req, event);
        LocalDateTime end = calEndTime(req, event, start);

        eventValidator.validateTime(start, end);

        // 변경 사항 전혀 없음
        if (!hasEventChanged(req) && !hasRecurrenceGroupChanged(req.recurrenceGroup())) {
            return;
        }

        Member member = event.getMember();

        // 단일 일정의 일정 수정인 경우
        if (event.getRecurrenceGroup() == null) {
            updateSingleEvent(req, event);
            if (req.recurrenceGroup() != null) {
                // 단일 일정에 반복 그룹을 추가하는 수정일때
                RecurrenceGroupReqDTO.CreateReq createReq =
                        RecurrenceGroupConverter.toCreateReq(req.recurrenceGroup());
                rgValidator.validateCreate(createReq, start);
                RecurrenceGroup rg = updateToRecurrenceEvent(req.recurrenceGroup(), member, start);
                event.updateRecurrenceGroup(rg);
            }
            return;
        }

        // 반복 그룹 수정할때만 validator 적용하기
        if (req.recurrenceGroup() != null)
            rgValidator.validateUpdate(req.recurrenceGroup(), event.getRecurrenceGroup(), start);

        // 수정범위가 있는 수정일 때
        switch (req.recurrenceUpdateScope()){
            case THIS_EVENT -> updateThisEventOnly(req, event, start);
            case THIS_AND_FOLLOWING_EVENTS -> updateThisAndFutureEvents(req, event, member, start, end);
            case ALL_EVENTS -> updateAllEvents(req, event, member, start, end);
            default -> throw new EventException(EventErrorCode.INVALID_UPDATE_SCOPE);
        }
    }

    @Override
    public void deleteEvent(Long eventId, LocalDate occurrenceDate, RecurrenceUpdateScope scope, Long memberId) {
        Event event = eventRepository.findByMemberIdAndId(memberId, eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        eventValidator.validateDelete(event, occurrenceDate ,scope);

        // 단일 일정일 경우
        if (event.getRecurrenceGroup() == null) {
            eventRepository.delete(event);
            return;
        }

        // 반복 그룹을 가진 일정일 경우
        switch (scope) {
            case THIS_EVENT -> {
                deleteThisEventOnly(event, occurrenceDate);
            }
            case THIS_AND_FOLLOWING_EVENTS -> {
                deleteThisAndFutureEvents(event, occurrenceDate);
            }
            case ALL_EVENTS -> {
                eventRepository.delete(event);
                recurrenceGroupRepository.delete(event.getRecurrenceGroup());
            }
            default -> throw new EventException(EventErrorCode.INVALID_UPDATE_SCOPE);
        }
    }

    // 반복그룹이 없는 일정을 수정할 경우
    private void updateSingleEvent(EventReqDTO.UpdateReq req, Event event) {
        event.update(
                req.title(),
                req.content(),
                req.startTime(),
                req.endTime(),
                req.location(),
                req.color(),
                req.isAllDay()
        );
    }

    private RecurrenceGroup updateToRecurrenceEvent(
            RecurrenceGroupReqDTO.UpdateReq req,
            Member member,
            LocalDateTime start) {
        RecurrenceGroupSpec rgSpec = RecurrenceGroupConverter.from(req, null, start);
        return createRecurrenceGroup(rgSpec, member);
    }

    // 반복 그룹이 있는 일정에서 해당 일정만 수정하는 경우
    private void updateThisEventOnly(
            EventReqDTO.UpdateReq req,
            Event event,
            LocalDateTime start
    ) {
        RecurrenceGroup rg = event.getRecurrenceGroup();
        RecurrenceException ex = RecurrenceGroupConverter.toRecurrenceExceptionForUpdate(req, rg, start);
        recurrenceExRepository.save(ex);
        rg.addExceptionDate(ex); // 해당 event가 속했던 반복 객체에 예외 날짜 추가
    }

    // 반복 그룹이 있는 일정에서 해당 일정만 삭제하는 경우
    private void deleteThisEventOnly(Event event, LocalDate start) {
        RecurrenceGroup rg = event.getRecurrenceGroup();
        RecurrenceException ex =
                RecurrenceGroupConverter.toRecurrenceExceptionForDelete(rg, start.atStartOfDay());
        recurrenceExRepository.save(ex);
        rg.addExceptionDate(ex); // 해당 event가 속했던 반복 객체에 예외 날짜 추가
    }

    // 반복 그룹이 있는 일정에서 해당 일정과 그 이후 일정들을 수정하는 경우
    private void updateThisAndFutureEvents(
            EventReqDTO.UpdateReq req,
            Event event,
            Member member,
            LocalDateTime start
    ) {
        RecurrenceGroup rg = event.getRecurrenceGroup();

        // 해당 event가 속한 반복그룹의 종료기간을 해당 event의 생성일 하루전으로 설정
        rg.updateEndDateTime(start);

        // 새 반복그룹을 가진 새 이벤트 생성
        Event newEvent = createEventWithNewRecurrenceGroup(req, event, member, start);

        // RecurrenceGroup 엔티티를 만들기 위한 내부 명세
        RecurrenceGroupSpec rgSpec = RecurrenceGroupConverter.from(req.recurrenceGroup(), rg, start);
        RecurrenceGroup newRg = createRecurrenceGroup(rgSpec, event.getMember());

        newEvent.updateRecurrenceGroup(newRg); // 연관관계 적용
        eventRepository.save(newEvent);
    }

    // 반복그룹이 있는 해당 일정과 그 이후 일정 삭제 하는 경우
    private void deleteThisAndFutureEvents(
            Event event,
            LocalDate occurrenceDate) {
        RecurrenceGroup rg = event.getRecurrenceGroup();
        // 해당 event가 속한 반복그룹의 종료기간을 해당 event의 생성일 하루전으로 설정
        rg.updateEndDateTime(occurrenceDate.atStartOfDay());
    }

    // 반복 그룹이 있는 일정에서 전체 일정 수정한느 경우
    private void updateAllEvents(
            EventReqDTO.UpdateReq req,
            Event event,
            Member member,
            LocalDateTime start
    ) {
        // 새 반복그룹을 가진 새 이벤트 생성
        Event newEvent = createEventWithNewRecurrenceGroup(req, event, member, start);

        // Event 엔티티를 만들기 위한 내부 명세
        EventSpec eventSpec = EventConverter.from(req, event, start, end);
        Event newEvent = createEvent(eventSpec, member, newRg);

        newEvent.updateRecurrenceGroup(newRg); // 새 이벤트에 새 반복 그룹 연관관계 설정

        // 기존 이벤트, 반복 그룹 삭제
        eventRepository.delete(event);
        recurrenceGroupRepository.delete(rg);
    }

    private RecurrenceGroup createRecurrenceGroup(
            RecurrenceGroupSpec rgSpec,
            Member member
    ) {
        RecurrenceGroup rg = RecurrenceGroupConverter.toRecurrenceGroup(rgSpec, member);
        recurrenceGroupRepository.save(rg);
        return rg;
    }

    private Event createEvent(
            EventSpec eventSpec,
            Member member,
            RecurrenceGroup baseRg
    ) {
      
        Event newEvent = EventConverter.toEvent(eventSpec, member, baseRg);
        eventRepository.save(newEvent);
        return newEvent;
    }

    private boolean hasEventChanged(EventReqDTO.UpdateReq req) {
        return req.title() != null
                || req.content() != null
                || req.startTime() != null
                || req.endTime() != null
                || req.location() != null
                || req.color() != null
                || req.isAllDay() != null;
    }

    private boolean hasRecurrenceGroupChanged(RecurrenceGroupReqDTO.UpdateReq req) {
        if (req == null) return false;

        return req.frequency() != null
                || req.endType() != null
                || req.endDate() != null
                || req.occurrenceCount() != null
                || req.monthlyType() != null
                || req.weekOfMonth() != null
                || req.monthOfYear() != null
                || req.daysOfWeek() != null
                || req.daysOfMonth() != null
                || req.dayOfWeekInMonth() != null
                || req.intervalValue() != null;
    }


    private LocalDateTime calStartTime(EventReqDTO.UpdateReq req, Event event) {
        if (req.startTime() != null) {
            return req.startTime();
        }

        // occurrenceDate가 있으면 그 날짜 + 기존 시간
        if (req.occurrenceDate() != null) {
            return req.occurrenceDate()
                    .atTime(event.getStartTime().toLocalTime());
        }

        // 둘 다 없으면 기존 event 값
        return event.getStartTime();
    }

    private LocalDateTime calEndTime(EventReqDTO.UpdateReq req, Event event, LocalDateTime calStartTime) {
        if (req.endTime() != null) {
            return req.endTime();
        }

        if (req.occurrenceDate() != null) {
            return req.occurrenceDate()
                    .atTime(event.getEndTime().toLocalTime());
        }

        // durationMinutes가 있으면 start 기준으로 계산 (추천)
        if (event.getDurationMinutes() != null) {
            return calStartTime.plusMinutes(event.getDurationMinutes());
        }

        return event.getEndTime();
    }
}
