package com.project.backend.domain.event.service.command;

import com.project.backend.domain.event.converter.EventConverter;
import com.project.backend.domain.event.converter.EventSpec;
import com.project.backend.domain.event.converter.RecurrenceGroupConverter;
import com.project.backend.domain.event.converter.RecurrenceGroupSpec;
import com.project.backend.domain.event.dto.AdjustedTime;
import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.ExceptionType;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.repository.RecurrenceExceptionRepository;
import com.project.backend.domain.event.repository.RecurrenceGroupRepository;
import com.project.backend.domain.event.service.RecurrenceTimeAdjuster;
import com.project.backend.domain.event.validator.EventValidator;
import com.project.backend.domain.event.validator.RecurrenceGroupValidator;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.reminder.enums.ChangeType;
import com.project.backend.domain.reminder.enums.ExceptionChangeType;
import com.project.backend.domain.reminder.listener.ReminderListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

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
    private final ReminderListener reminderListener;

    @Override
    public EventResDTO.CreateRes createEvent(EventReqDTO.CreateReq req, Long memberId) {
        eventValidator.validateCreate(req);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        RecurrenceGroup recurrenceGroup = null;
        RecurrenceGroupSpec rgSpec;

        if (req.recurrenceGroup() != null) {

            rgValidator.validateCreate(req.recurrenceGroup(), req.startTime());

            rgSpec = RecurrenceGroupConverter.from(req.recurrenceGroup(), req.startTime());
            recurrenceGroup = createRecurrenceGroup(rgSpec, member);
        }

        EventSpec eventSpec = EventConverter.from(req, req.startTime(), req.endTime());
        Event event = createEvent(eventSpec, member, recurrenceGroup);

        // 이벤트 생성에 따른 리스너 생성 로직 실행
        handleEventChanged(
                event.getId(),
                memberId,
                event.getTitle(),
                recurrenceGroup != null,
                event.getStartTime(),
                null,
                ChangeType.CREATED
        );
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
                RecurrenceGroup rg = updateToRecurrenceEvent(req, req.recurrenceGroup(), member, start);
                event.updateRecurrenceGroup(rg);
                rg.updateEvent(event);
                // 이벤트 + 반복 생성에 따른 리스너 수정 로직 실행
                handleEventChanged(
                        eventId,
                        memberId,
                        event.getTitle(),
                        true,
                        start,
                        null,
                        ChangeType.UPDATE_ADD_RECURRENCE
                );
            } else{
                // 이벤트 생성에 따른 리스너 생성 수정 실행
                handleEventChanged(
                        eventId,
                        memberId,
                        event.getTitle(),
                        false,
                        start,
                        null,
                        ChangeType.UPDATE_SINGLE);
            }
            return;
        }

        // 반복 그룹 수정할때만 validator 적용하기
        if (req.recurrenceGroup() != null)
            rgValidator.validateUpdate(req.recurrenceGroup(), event.getRecurrenceGroup(), start);

        // 수정범위가 있는 수정일 때
        switch (req.recurrenceUpdateScope()){
            case THIS_EVENT -> updateThisEventOnly(req, event, member);
            case THIS_AND_FOLLOWING_EVENTS -> {
                if (req.startTime() == null && req.endTime() == null && req.recurrenceGroup() == null) {

                }
                updateThisAndFutureEvents(req, event, member, start, end);
            }
            default -> throw new EventException(EventErrorCode.INVALID_UPDATE_SCOPE);
        }
    }

    @Override
    public void deleteEvent(Long eventId, LocalDate occurrenceDate, RecurrenceUpdateScope scope, Long memberId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        eventValidator.validateDelete(event, occurrenceDate ,scope);

        // 단일 일정일 경우
        if (event.getRecurrenceGroup() == null) {
            eventRepository.delete(event);
            handleEventChanged(
                    eventId,
                    memberId,
                    null,
                    false,
                    event.getStartTime(),
                    null,
                    ChangeType.DELETED_SINGLE);
            return;
        }

        // 반복 그룹을 가진 일정일 경우
        switch (scope) {
            case THIS_EVENT -> {
                deleteThisEventOnly(event, occurrenceDate, memberId);
            }
            case THIS_AND_FOLLOWING_EVENTS -> {
                deleteThisAndFutureEvents(event, memberId, occurrenceDate);
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
            EventReqDTO.UpdateReq eventReq,
            RecurrenceGroupReqDTO.UpdateReq rgReq,
            Member member,
            LocalDateTime start) {
        RecurrenceGroupSpec rgSpec = RecurrenceGroupConverter.from(eventReq, rgReq, null, start);
        return createRecurrenceGroup(rgSpec, member);
    }

    // 반복 그룹이 있는 일정에서 해당 일정만 수정하는 경우
    private void updateThisEventOnly(
            EventReqDTO.UpdateReq req,
            Event event,
            Member member
    ) {
        RecurrenceGroup rg = event.getRecurrenceGroup();
        RecurrenceException ex = RecurrenceGroupConverter.toRecurrenceExceptionForUpdate(req, rg);
        recurrenceExRepository.save(ex);
        rg.addExceptionDate(ex); // 해당 event가 속했던 반복 객체에 예외 날짜 추가

        if (ex.getStartTime() != null || ex.getTitle() != null) {
            // 해당 일정만 수정했을 때 리스너 수정 로직 실행
            handleExceptionChanged(
                    ex.getId(),
                    event.getId(),
                    member.getId(),
                    ex.getTitle() != null ? ex.getTitle() : event.getTitle(),
                    event.getStartTime(),
                    ExceptionChangeType.UPDATED_THIS);
        }
    }

    // 반복 그룹이 있는 일정에서 해당 일정만 삭제하는 경우
    private void deleteThisEventOnly(Event event, LocalDate start, Long memberId) {
        RecurrenceGroup rg = event.getRecurrenceGroup();
        RecurrenceException ex =
                recurrenceExRepository.
                        findByRecurrenceGroupIdAndStartDateAndExceptionType(rg.getId(), start, ExceptionType.OVERRIDE)
                        .map(existingEx -> {
                            // 이미 존재한다면 타입만 변경 (더티 체킹 활용)
                            existingEx.updateExceptionTypeToSKIP();
                            return existingEx;
                        })
                        .orElseGet(() -> {
                            // 존재하지 않는다면 새로 생성 후 저장
                            RecurrenceException newEx = RecurrenceGroupConverter
                                    .toRecurrenceExceptionForDelete(rg, start.atStartOfDay());
                            rg.addExceptionDate(newEx); // 연관관계 편의 메서드 호출
                            return recurrenceExRepository.save(newEx);
                        });
        // 해당 일정만 삭제했을 때 리스너 수정 로직 실행
        handleExceptionChanged(
                ex.getId(),
                event.getId(),
                memberId,
                ex.getTitle(),
                ex.getStartTime(),
                ExceptionChangeType.DELETED_THIS);
    }

    // 반복 그룹이 있는 일정에서 해당 일정과 그 이후 일정들을 수정하는 경우
    private void updateThisAndFutureEvents(
            EventReqDTO.UpdateReq req,
            Event event,
            Member member,
            LocalDateTime start,
            LocalDateTime end
    ) {
        RecurrenceGroup rg = event.getRecurrenceGroup();

        // 새 반복그룹을 가진 새 이벤트 생성
        Event newEvent = createEventWithNewRecurrenceGroup(req, event, member, start, end);

        // 해당 event가 속한 반복그룹의 종료기간을 해당 event의 생성일 하루전으로 설정
        rg.updateEndDateTime(start);

        // 해당 일정과 그 이후 일정들을 수정했을 때 리스너 수정 로직 실행
        // 기존 일정에 대한 리마인더 삭제 여부 결정
        handleRecurrenceEnded(
                event.getId(),
                newEvent.getStartTime()
        );

        // 원본 일정에 대한 수정이면 기존 일정 + 반복 삭제
        if (Objects.equals(event.getStartTime(), req.occurrenceDate().atTime(event.getStartTime().toLocalTime()))) {
            eventRepository.delete(event);
            recurrenceGroupRepository.delete(event.getRecurrenceGroup());
            handleEventChanged(
                    event.getId(),
                    member.getId(),
                    null,
                    true,
                    event.getStartTime(),
                    null,
                    ChangeType.DELETED_ALL);
        }

        // 새 일정 생성
        handleEventChanged(
                newEvent.getId(),
                member.getId(),
                newEvent.getTitle(),
                true,
                newEvent.getStartTime(),
                null,
                ChangeType.CREATED
        );
    }

    // 반복그룹이 있는 해당 일정과 그 이후 일정 삭제 하는 경우
    private void deleteThisAndFutureEvents(
            Event event,
            Long memberId,
            LocalDate occurrenceDate) {

        RecurrenceGroup rg = event.getRecurrenceGroup();

        // 삭제하려는 날을 포함한 이후 일정들에 대한 반복예외 객체 모두 삭제
        recurrenceExRepository.deleteByRecurrenceGroupIdAndOccurrenceDate(rg.getId(), occurrenceDate);

        // 원본 일정에 대한 수정이면 기존 일정 + 반복 삭제
        if (event.getStartTime().toLocalDate().equals(occurrenceDate)) {
            eventRepository.delete(event);
            recurrenceGroupRepository.delete(event.getRecurrenceGroup());
            handleEventChanged(
                    event.getId(),
                    memberId,
                    null,
                    true,
                    event.getStartTime(),
                    occurrenceDate, // 삭제하려는 대상 일정 날짜
                    ChangeType.DELETED_ALL);
        } else {
            // 해당 event가 속한 반복그룹의 종료기간을 해당 event의 생성일 하루전으로 설정
            rg.updateEndDateTime(occurrenceDate.atStartOfDay());

            // 해당 일정과 그 이후 일정들을 삭제했을 때 리스너 수정 로직 실행
            handleEventChanged(
                    event.getId(),
                    memberId,
                    null,
                    true,
                    null,
                    occurrenceDate, // 삭제하려는 대상 일정 날짜
                    ChangeType.DELETED_THIS_AND_FOLLOWING
            );
        }
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

        if (baseRg != null) {
            baseRg.attachEvent(newEvent);
        }

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

        return req.occurrenceDate().atTime(event.getStartTime().toLocalTime());
    }

    private LocalDateTime calEndTime(EventReqDTO.UpdateReq req, Event event, LocalDateTime calStartTime) {
        if (req.endTime() != null) {
            return req.endTime();
        }

        // durationMinutes가 있으면 start 기준으로 계산
        if (event.getDurationMinutes() != null) {
            return calStartTime.plusMinutes(event.getDurationMinutes());
        }

        if (req.occurrenceDate() != null) {
            return req.occurrenceDate()
                    .atTime(event.getEndTime().toLocalTime());
        }

        return event.getEndTime();
    }

    private void handleEventChanged (
            Long eventId,
            Long memberId,
            String title,
            Boolean isRecurring,
            LocalDateTime startTime,
            LocalDate startDate,
            ChangeType changeType
    ) {
        reminderListener.onEvent(EventConverter.toEventChanged(
                eventId,
                memberId,
                title,
                isRecurring,
                startTime,
                startDate,
                changeType
        ));
    }

    private void handleExceptionChanged (
            Long exceptionId,
            Long eventId,
            Long memberId,
            String title,
            LocalDateTime occurrenceTime,
            ExceptionChangeType changeType
    ) {
        reminderListener.onEvent(EventConverter.toRecurrenceExceptionChanged(
                exceptionId,
                eventId,
                memberId,
                title,
                true,
                occurrenceTime,
                changeType
        ));
    }

    private void handleRecurrenceEnded(Long eventId, LocalDateTime startTime) {
        reminderListener.onEvent(EventConverter.toEventRecurrenceEnded(eventId, startTime));
    }

    private Event createEventWithNewRecurrenceGroup(
            EventReqDTO.UpdateReq req,
            Event baseEvent,
            Member member,
            LocalDateTime baseStart,
            LocalDateTime baseEnd
    ) {
        // RecurrenceGroupSpec
        RecurrenceGroupSpec rgSpec =
                RecurrenceGroupConverter.from(
                        req,
                        req.recurrenceGroup(),
                        baseEvent.getRecurrenceGroup(),
                        baseStart
                );

        RecurrenceGroup newRg = createRecurrenceGroup(rgSpec, member);

        // 2. start/end 결정
        LocalDateTime finalStart = baseStart;
        LocalDateTime finalEnd = baseEnd;

        // 반복 규칙 기준 보정 (startTime이 있다면, startTime에 대한 요일, 일, 주, 월 보정이 spec 생성에서 완료됨)
        if (req.startTime() == null) {
            AdjustedTime adjusted = RecurrenceTimeAdjuster.adjust(baseStart, baseEnd, rgSpec);
            finalStart = adjusted.start();
            finalEnd = adjusted.end();
        }

        // 3. Event 생성
        EventSpec eventSpec =
                EventConverter.from(req, baseEvent, finalStart, finalEnd);

        Event newEvent = EventConverter.toEvent(eventSpec, member, newRg);

        // 연관관계 설정
        newEvent.updateRecurrenceGroup(newRg);
        newRg.attachEvent(newEvent);

        eventRepository.save(newEvent);

        return newEvent;
    }
}
