package com.project.backend.domain.event.service.command;

import com.project.backend.domain.common.reminder.bridge.ReminderEventBridge;
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
import com.project.backend.domain.event.service.EventOccurrenceResolver;
import com.project.backend.domain.event.service.RecurrenceTimeAdjuster;
import com.project.backend.domain.event.validator.EventValidator;
import com.project.backend.domain.event.validator.RecurrenceGroupValidator;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.reminder.enums.ChangeType;
import com.project.backend.domain.reminder.enums.DeletedType;
import com.project.backend.domain.reminder.enums.ExceptionChangeType;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.suggestion.enums.SuggestionInvalidateReason;
import com.project.backend.domain.suggestion.publisher.SuggestionInvalidatePublisher;
import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import com.project.backend.domain.suggestion.vo.fingerprint.EventFingerPrint;
import com.project.backend.domain.suggestion.vo.fingerprint.RecurrenceGroupFingerPrint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

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
    private final EventOccurrenceResolver eventOccurrenceResolver;
    private final ReminderEventBridge reminderEventBridge;
    private final SuggestionInvalidatePublisher suggestionInvalidatePublisher;
    private final SuggestionRepository suggestionRepository;

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
        reminderEventBridge.handlePlanChanged(
                event.getId(),
                TargetType.EVENT,
                memberId,
                event.getTitle(),
                recurrenceGroup != null,
                event.getStartTime(),
                ChangeType.CREATED
        );
        // 반복의 유무와 상관없이 동일한 이름 + 장소로 생성된 이벤트가 있으면 비활성화
        byte[] createdHash = suggestionInvalidatePublisher.eventHash(event.getTitle(), event.getLocation());
        log.info("event created");
        suggestionInvalidatePublisher.publish(memberId, SuggestionInvalidateReason.EVENT_CREATED, createdHash);

        return EventConverter.toCreateRes(event);
    }

    @Override
    public void updateEvent(
            EventReqDTO.UpdateReq req,
            Long eventId,
            Long memberId,
            RecurrenceUpdateScope scope,
            LocalDateTime occurrenceDate
    ) {
        // 변경 사항 전혀 없음
        if (!hasEventChanged(req) && !hasRecurrenceGroupChanged(req.recurrenceGroup())) {
            return;
        }

        Event event = eventRepository.findByIdAndMemberId(eventId, memberId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        eventValidator.validateUpdate(event, occurrenceDate, scope);
        byte[] beforeHash = suggestionInvalidatePublisher.eventHash(event.getTitle(), event.getLocation());
        EventFingerPrint beforeFingerPrint = EventFingerPrint.from(event);

        // 수정안한 계산된 일정의 날짜인지, 수정된 날짜인지 계산
        LocalDateTime start = event.isRecurring() ? calStartTime(req, event, occurrenceDate): event.getStartTime();
        LocalDateTime end = calEndTime(req, event, start, occurrenceDate);

        eventValidator.validateTime(start, end);

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
                reminderEventBridge.handlePlanChanged(
                        eventId,
                        TargetType.EVENT,
                        memberId,
                        event.getTitle(),
                        true,
                        start,
                        ChangeType.UPDATE_ADD_RECURRENCE
                );
            } else{
                // 이벤트 생성에 따른 리스너 생성 수정 실행
                reminderEventBridge.handlePlanChanged(
                        eventId,
                        TargetType.EVENT,
                        memberId,
                        event.getTitle(),
                        false,
                        start,
                        ChangeType.UPDATE_SINGLE);

            }
            return;
        }

        // occurrenceDate가 존재하는 일정의 계산된 날짜인지
        eventOccurrenceResolver.assertOccurrenceExists(event, occurrenceDate);

        // 반복 그룹 수정할때만 validator 적용하기
        if (req.recurrenceGroup() != null)
            rgValidator.validateUpdate(req.recurrenceGroup(), event.getRecurrenceGroup(), start);

        // 수정범위가 있는 수정일 때
        switch (scope){
            case THIS_EVENT -> updateThisEventOnly(req, event, member, occurrenceDate);
            case THIS_AND_FOLLOWING_EVENTS -> updateThisAndFutureEvents(req, event, member, start, end, occurrenceDate);
            default -> throw new EventException(EventErrorCode.INVALID_UPDATE_SCOPE);
        }

        // 이벤트 수정에 따른 제안 만료 이벤트 퍼블리싱
        byte[] afterHash = suggestionInvalidatePublisher.eventHash(event.getTitle(), event.getLocation());
        EventFingerPrint afterFingerPrint = EventFingerPrint.from(event);

        boolean keyChanged = suggestionInvalidatePublisher.isHashChanged(beforeHash, afterHash);
        boolean fingerPrintChanged = !beforeFingerPrint.equals(afterFingerPrint);
        log.info("keyChanged: {}, fingerPrintChanged: {}", keyChanged, fingerPrintChanged);

        // 키 변경 OR 시간(등) 변경이면 무효화
        if (keyChanged || fingerPrintChanged) {
            suggestionInvalidatePublisher.publish(memberId, SuggestionInvalidateReason.EVENT_UPDATED, beforeHash);
        }
    }

    @Override
    public void deleteEvent(
            Long eventId,
            LocalDateTime occurrenceDate,
            RecurrenceUpdateScope scope,
            Long memberId
    ) {
        Event event = eventRepository.findByIdAndMemberId(eventId, memberId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        eventValidator.validateDelete(event, occurrenceDate ,scope);

        // 단일 일정일 경우
        if (event.getRecurrenceGroup() == null) {
            eventRepository.delete(event);
            reminderEventBridge.handleReminderDeleted(
                    null,
                    memberId,
                    occurrenceDate,
                    eventId,
                    TargetType.EVENT,
                    DeletedType.DELETED_SINGLE);
            return;
        }

        // occurrenceDate가 존재하는 일정의 계산된 날짜인지
        eventOccurrenceResolver.assertOccurrenceExists(event, occurrenceDate);

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

    private void updateRecurrenceException(EventReqDTO.UpdateReq req, RecurrenceException re) {
        re.updateForUpdate(
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
            Member member,
            LocalDateTime occurrenceDate
    ) {
        RecurrenceGroup rg = event.getRecurrenceGroup();

        // 만약 이미 수정된 일정을 또 수정하는 경우
        Optional<RecurrenceException> re = recurrenceExRepository
                .findByRecurrenceGroupIdAndExceptionDateAndExceptionType(
                        rg.getId(),
                        occurrenceDate,
                        ExceptionType.OVERRIDE
                );

        if (re.isPresent()) {
            RecurrenceException ex = re.get();
            updateRecurrenceException(req, ex);
            reminderEventBridge.handleExceptionChanged(
                    ex.getId(),
                    event.getId(),
                    TargetType.EVENT,
                    member.getId(),
                    ex.getTitle() != null ? ex.getTitle() : event.getTitle(),
                    ex.getStartTime() != null ? ex.getStartTime() : ex.getExceptionDate(),
                    ExceptionChangeType.UPDATE_THIS_AGAIN
            );
            return;
        }

        RecurrenceException ex = RecurrenceGroupConverter.toRecurrenceExceptionForUpdate(req, rg, occurrenceDate);
        recurrenceExRepository.save(ex);
        rg.addExceptionDate(ex); // 해당 event가 속했던 반복 객체에 예외 날짜 추가

        // 해당 일정만 수정했을 때 리스너 수정 로직 실행
        reminderEventBridge.handleExceptionChanged(
                ex.getId(),
                event.getId(),
                TargetType.EVENT,
                member.getId(),
                ex.getTitle() != null ? ex.getTitle() : event.getTitle(),
                ex.getStartTime() != null ? ex.getStartTime() : ex.getExceptionDate(),
                ExceptionChangeType.UPDATED_THIS);
    }

    // 반복 그룹이 있는 일정에서 해당 일정만 삭제하는 경우
    private void deleteThisEventOnly(Event event, LocalDateTime occurrenceDate, Long memberId) {
        RecurrenceGroup rg = event.getRecurrenceGroup();

        Optional<RecurrenceException> r = recurrenceExRepository
                .findByRecurrenceGroupIdAndExceptionDateAndExceptionType(
                        event.getRecurrenceGroup().getId(), occurrenceDate, ExceptionType.OVERRIDE
                );

        // THIS_EVENT로 수정된 일정을 삭제하는 경우
        if (r.isPresent()) {
            RecurrenceException ex = r.get();
            ex.updateExceptionTypeToSKIP();
            LocalDateTime startTime = ex.getStartTime() != null ? ex.getStartTime() : ex.getExceptionDate();
            if (startTime.isAfter(LocalDateTime.now())) {
                reminderEventBridge.handleReminderDeleted(
                        ex.getId(),
                        memberId,
                        startTime,
                        event.getId(),
                        TargetType.EVENT,
                        DeletedType.DELETED_SINGLE
                );
            }
            return;
        }

        // 존재하지 않는다면 새로 생성 후 저장
        RecurrenceException newEx = RecurrenceGroupConverter.toRecurrenceExceptionForDelete(rg, occurrenceDate);
        rg.addExceptionDate(newEx); // 연관관계 편의 메서드 호출
        recurrenceExRepository.save(newEx);

        // 해당 일정만 삭제했을 때 리스너 수정 로직 실행
        reminderEventBridge.handleExceptionChanged(
                newEx.getId(),
                event.getId(),
                TargetType.EVENT,
                memberId,
                event.getTitle(),
                occurrenceDate,
                ExceptionChangeType.DELETED_THIS);
    }

    // 반복 그룹이 있는 일정에서 해당 일정과 그 이후 일정들을 수정하는 경우
    private void updateThisAndFutureEvents(
            EventReqDTO.UpdateReq req,
            Event event,
            Member member,
            LocalDateTime start,
            LocalDateTime end,
            LocalDateTime occurrenceDate
    ) {
        RecurrenceGroup rg = event.getRecurrenceGroup();

        // 새 반복그룹을 가진 새 이벤트 생성
        Event newEvent = createEventWithNewRecurrenceGroup(req, event, member, start, end);

        // 수정하려는 날짜 포함한 이후 일정들에 대한 반복예외 객체 모두 삭제
        recurrenceExRepository.deleteByRecurrenceGroupIdAndOccurrenceDate(rg.getId(), occurrenceDate);

        // 원본 일정에 대한 수정이면 기존 일정 + 반복 삭제
        if (Objects.equals(event.getStartTime(), occurrenceDate)) {
            eventRepository.delete(event);
            recurrenceGroupRepository.delete(event.getRecurrenceGroup());
            reminderEventBridge.handleReminderDeleted(
                    null,
                    member.getId(),
                    event.getStartTime(),
                    event.getId(),
                    TargetType.EVENT,
                    DeletedType.DELETED_ALL);
        } else {
            // 해당 event가 속한 반복그룹의 종료기간을 해당 event의 생성일 하루전으로 설정
            rg.updateEndDateTime(occurrenceDate);

            // 해당 일정과 그 이후 일정들을 수정했을 때 리스너 수정 로직 실행
            // 기존 일정에 대한 리마인더 삭제 여부 결정
            reminderEventBridge.handleReminderDeleted(
                    null,
                    member.getId(),
                    occurrenceDate,
                    event.getId(),
                    TargetType.EVENT,
                    DeletedType.DELETED_THIS_AND_FOLLOWING
            );
        }

        // 새 일정 생성에 대한 리마인더 발생
        reminderEventBridge.handlePlanChanged(
                newEvent.getId(),
                TargetType.EVENT,
                member.getId(),
                newEvent.getTitle(),
                true,
                newEvent.getStartTime(),
                ChangeType.CREATED
        );
    }

    // 반복그룹이 있는 해당 일정과 그 이후 일정 삭제 하는 경우
    private void deleteThisAndFutureEvents(
            Event event,
            Long memberId,
            LocalDateTime occurrenceDate) {
        RecurrenceGroup rg = event.getRecurrenceGroup();

        // 삭제하려는 날짜가 수정된 일정인지
        Optional<RecurrenceException> re = recurrenceExRepository.
                findByRecurrenceGroupIdAndExceptionDate(rg.getId(), occurrenceDate);

        LocalDateTime startDate = occurrenceDate;

        // 수정된 일정에 대한 occurrenceDate라면
        if (re.isPresent()) {
            RecurrenceException ex = re.get();
            startDate = ex.getStartTime() != null ? ex.getStartTime() : ex.getExceptionDate();
        }

        // 삭제하려는 날을 포함한 이후 일정들에 대한 반복예외 객체 모두 삭제
        recurrenceExRepository.deleteByRecurrenceGroupIdAndOccurrenceDate(rg.getId(), occurrenceDate);

        // 원본 일정에 대한 수정이면 기존 일정 + 반복 삭제
        if (event.getStartTime().equals(occurrenceDate)) {
            eventRepository.delete(event);
            recurrenceGroupRepository.delete(event.getRecurrenceGroup());
            reminderEventBridge.handleReminderDeleted(
                    null,
                    memberId,
                    event.getStartTime(),
                    event.getId(),
                    TargetType.EVENT,
                    DeletedType.DELETED_ALL
            );
        } else {
            // 계산된 일정에 대한 수정이면 해당 event가 속한 반복그룹의 종료기간을 해당 event의 생성일 하루전으로 설정
            rg.updateEndDateTime(startDate.toLocalDate().atStartOfDay().minusDays(1));

            // 해당 일정과 그 이후 일정들을 삭제했을 때 리스너 수정 로직 실행
            reminderEventBridge.handleReminderDeleted(
                    null,
                    memberId,
                    startDate,
                    event.getId(),
                    TargetType.EVENT,
                    DeletedType.DELETED_THIS_AND_FOLLOWING
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


    private LocalDateTime calStartTime(EventReqDTO.UpdateReq req, Event event, LocalDateTime occurrenceDate) {
        if (req.startTime() != null) {
            return req.startTime();
        }

        Optional<RecurrenceException> re = recurrenceExRepository.
                findByRecurrenceGroupIdAndExceptionDateAndExceptionType(
                event.getRecurrenceGroup().getId(), occurrenceDate, ExceptionType.OVERRIDE
        );
        if (re.isPresent() && re.get().getStartTime() != null) {
            return re.get().getStartTime();
        }

        return occurrenceDate;
    }

    private LocalDateTime calEndTime(
            EventReqDTO.UpdateReq req,
            Event event,
            LocalDateTime calStartTime,
            LocalDateTime occurrenceDate
            ) {
        if (req.endTime() != null) {
            return req.endTime();
        }

        // durationMinutes가 있으면 start 기준으로 계산
        if (event.getDurationMinutes() != null) {
            return calStartTime.plusMinutes(event.getDurationMinutes());
        }

        return occurrenceDate;
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
