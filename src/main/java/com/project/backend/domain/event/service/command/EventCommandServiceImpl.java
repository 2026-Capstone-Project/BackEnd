package com.project.backend.domain.event.service.command;

import com.project.backend.domain.common.reminder.bridge.ReminderEventBridge;
import com.project.backend.domain.event.converter.*;
import com.project.backend.domain.event.dto.AdjustedTime;
import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.entity.*;
import com.project.backend.domain.event.enums.EventColor;
import com.project.backend.domain.event.enums.ExceptionType;
import com.project.backend.domain.common.plan.enums.MonthlyWeekdayRule;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.repository.*;
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
import com.project.backend.domain.suggestion.invalidation.dispatcher.SuggestionInvalidationDispatcher;
import com.project.backend.domain.suggestion.invalidation.factory.EventSuggestionSnapshotFactory;
import com.project.backend.domain.suggestion.invalidation.planner.InvalidationPlan;
import com.project.backend.domain.suggestion.invalidation.planner.SuggestionInvalidationPlanner;
import com.project.backend.domain.suggestion.invalidation.snapshot.EventSuggestionSnapshot;
import com.project.backend.domain.suggestion.util.SuggestionKeyUtil;
import com.project.backend.global.recurrence.util.RecurrenceUtils;
import com.project.backend.domain.suggestion.enums.SuggestionInvalidateReason;
import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventCommandServiceImpl implements EventCommandService {

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final EventTitleHistoryRepository eventTitleHistoryRepository;
    private final RecurrenceExceptionRepository recurrenceExRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final EventValidator eventValidator;
    private final RecurrenceGroupValidator rgValidator;
    private final EventOccurrenceResolver eventOccurrenceResolver;
    private final ReminderEventBridge reminderEventBridge;
    private final SuggestionRepository suggestionRepository;
    private final EventSuggestionSnapshotFactory eventSuggestionSnapshotFactory;
    private final SuggestionInvalidationPlanner suggestionInvalidationPlanner;
    private final SuggestionInvalidationDispatcher suggestionInvalidationDispatcher;
    private final EventLocationHistoryRepository eventLocationHistoryRepository;

    @Override
    public EventResDTO.CreateRes createEvent(EventReqDTO.CreateReq req, Long memberId) {
        eventValidator.validateCreate(req);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        RecurrenceGroup recurrenceGroup = null;
        RecurrenceGroupSpec rgSpec;

        // 반복 일정 생성일 때
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
        EventSuggestionSnapshot createdSnapshot = eventSuggestionSnapshotFactory.from(event);

        InvalidationPlan invalidationPlan = suggestionInvalidationPlanner.planForCreate(
                createdSnapshot,
                SuggestionInvalidateReason.EVENT_CREATED
        );

        log.info("event created");
        suggestionInvalidationDispatcher.dispatch(memberId, invalidationPlan);

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
        if (!hasAnyEventFieldProvided(req) && !hasAnyRecurrenceGroupFieldProvided(req.recurrenceGroup())) {
            log.info("1.Event update request is not changed. eventId: {}", eventId);
            return;
        }

        Event event = eventRepository.findByIdAndMemberId(eventId, memberId)
                .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

        eventValidator.validateUpdate(event, req.recurrenceGroup(), occurrenceDate, scope);

        EventSuggestionSnapshot beforeSnapshot = eventSuggestionSnapshotFactory.from(event);

        // 수정안한 계산된 일정의 날짜인지, 수정된 날짜인지 계산
        LocalDateTime start = calStartTime(req, event, occurrenceDate);
        LocalDateTime end = calEndTime(req, event, start, occurrenceDate);

        eventValidator.validateTime(start, end);
        eventValidator.validateBlank(req);

        // 입력한 값이 기존 단일 일정 or 반복 일정의 필드값과 동일한 경우
        if (!hasEventChanged(event, req, start, end, occurrenceDate)
                && !hasEventRecurrenceChanged(event.getRecurrenceGroup(), req.recurrenceGroup())) {
            log.info("2.Event update request is not changed. eventId: {}", eventId);
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
                RecurrenceGroup rg = updateToRecurrenceEvent(req, req.recurrenceGroup(), event, member, start);
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
            // 수정 시 history upsert
            upsertEventTitleHistory(req.title(), memberId);
            upsertEventLocationHistory(req.location(), memberId);

            // 단일 이벤트 after 스냅샷
            EventSuggestionSnapshot afterSnapshot = eventSuggestionSnapshotFactory.from(event);
            log.info("EventCommandImpl, after 스냅샷 생성 완료");

            InvalidationPlan invalidationPlan = suggestionInvalidationPlanner.planForUpdate(
                    beforeSnapshot,
                    afterSnapshot,
                    SuggestionInvalidateReason.EVENT_UPDATED,
                    SuggestionInvalidateReason.RECURRENCE_GROUP_UPDATED,
                    SuggestionInvalidateReason.RECURRENCE_GROUP_UPDATED
            );

            suggestionInvalidationDispatcher.dispatch(memberId, invalidationPlan);

            return;
        }

        // occurrenceDate가 존재하는 일정의 계산된 날짜인지
        eventOccurrenceResolver.assertOccurrenceExists(event, occurrenceDate);

        // 반복 그룹 수정할때만 validator 적용하기
        if (req.recurrenceGroup() != null)
            rgValidator.validateUpdate(req.recurrenceGroup(), event.getRecurrenceGroup(), start);

        // TODO : 임시
        // 모객체를 건드려서 완전 삭제되는 경우인가?
        boolean hardDeleteGroup =
                scope == RecurrenceUpdateScope.THIS_AND_FOLLOWING_EVENTS
                        && event.getStartTime().equals(occurrenceDate);

        if (hardDeleteGroup) { // Suggestion 객체의 FK 연결해제
            Long rgId = event.getRecurrenceGroup().getId();
            suggestionRepository.detachPreviousEvent(memberId, eventId);
            suggestionRepository.detachRecurrenceGroup(memberId, rgId);
        }

        // THIS_AND_FOLLOWING이면 after 대상이 newEvent일 수 있음
        Event afterBase = event;

        // 수정범위가 있는 수정일 때
        switch (scope){
            case THIS_EVENT -> updateThisEventOnly(req, event, member, occurrenceDate);
            case THIS_AND_FOLLOWING_EVENTS -> afterBase = updateThisAndFutureEvents(req, event, member, start, end, occurrenceDate);
            default -> throw new EventException(EventErrorCode.INVALID_UPDATE_SCOPE);
        }

        // 수정 시 history upsert
        upsertEventTitleHistory(req.title(), memberId);
        upsertEventLocationHistory(req.location(), memberId);

        // 모객체 이후 전체로 업데이트 한 경우 새로운 반복 그룹이 생성되므로 삭제 이유는 반복 삭제, 그 외의 경우에는 반복 업데이트
        SuggestionInvalidateReason beforeRgReason =
                hardDeleteGroup
                        ? SuggestionInvalidateReason.RECURRENCE_GROUP_DELETED
                        : SuggestionInvalidateReason.RECURRENCE_GROUP_UPDATED;

        SuggestionInvalidateReason afterRgReason = SuggestionInvalidateReason.RECURRENCE_GROUP_UPDATED;

        EventSuggestionSnapshot afterSnapshot = eventSuggestionSnapshotFactory.from(afterBase);

        InvalidationPlan invalidationPlan = suggestionInvalidationPlanner.planForUpdate(
                beforeSnapshot,
                afterSnapshot,
                SuggestionInvalidateReason.EVENT_UPDATED,
                beforeRgReason,
                afterRgReason
        );

        suggestionInvalidationDispatcher.dispatch(memberId, invalidationPlan);
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

        EventSuggestionSnapshot beforeSnapshot = eventSuggestionSnapshotFactory.from(event);

        // 단일 일정일 경우
        if (event.getRecurrenceGroup() == null) {
            suggestionRepository.detachPreviousEvent(memberId, eventId);
            eventRepository.delete(event);
            reminderEventBridge.handleReminderDeleted(
                    null,
                    memberId,
                    occurrenceDate,
                    eventId,
                    TargetType.EVENT,
                    DeletedType.DELETED_SINGLE);
            // 단일은 그냥 해시 겹치면 바로 만료
            InvalidationPlan invalidationPlan = suggestionInvalidationPlanner.planForDelete(
                    beforeSnapshot,
                    SuggestionInvalidateReason.EVENT_DELETED,
                    null,
                    true,
                    false
            );

            log.info("event deleted");
            suggestionInvalidationDispatcher.dispatch(memberId, invalidationPlan);
            return;
        }

        // occurrenceDate가 존재하는 일정의 계산된 날짜인지
        eventOccurrenceResolver.assertOccurrenceExists(event, occurrenceDate);

        // 객체를 지워야 할 상황인지
        boolean hardDeleteGroup =
                scope == RecurrenceUpdateScope.THIS_AND_FOLLOWING_EVENTS
                        && event.getStartTime().equals(occurrenceDate);

        // hard delete면 event + rg 둘 다 FK 걸릴 수 있으니 둘 다 detach
        if (hardDeleteGroup) {
            Long rgId = event.getRecurrenceGroup().getId();
            suggestionRepository.detachPreviousEvent(memberId, eventId);
            suggestionRepository.detachRecurrenceGroup(memberId, rgId);
        }

        // 반복 그룹을 가진 일정일 경우
        switch (scope) {
            case THIS_EVENT -> deleteThisEventOnly(event, occurrenceDate, memberId);
            case THIS_AND_FOLLOWING_EVENTS -> deleteThisAndFutureEvents(event, memberId, occurrenceDate);
            default -> throw new EventException(EventErrorCode.INVALID_UPDATE_SCOPE);
        }

        // reason 결정
        SuggestionInvalidateReason reason;
        if (scope == RecurrenceUpdateScope.THIS_EVENT) {
            // 그 날만 skip/override -> 그룹 삭제가 아니라 변경임
            reason = SuggestionInvalidateReason.RECURRENCE_GROUP_UPDATED;
        } else {
            // THIS_AND_FOLLOWING_EVENT
            reason = hardDeleteGroup
                    ? SuggestionInvalidateReason.RECURRENCE_GROUP_DELETED
                    : SuggestionInvalidateReason.RECURRENCE_GROUP_UPDATED;
        }

        // 반복은 RGH 축 무조건 정리
        InvalidationPlan invalidationPlan = suggestionInvalidationPlanner.planForDelete(
                beforeSnapshot,
                SuggestionInvalidateReason.EVENT_DELETED,
                reason,
                hardDeleteGroup,
                true
        );

        if (hardDeleteGroup) {
            log.info("event deleted");
        } else {
            log.info("before rg deleted");
        }

        suggestionInvalidationDispatcher.dispatch(memberId, invalidationPlan);
    }

    // 반복그룹이 없는 일정을 수정할 경우
    private void updateSingleEvent(EventReqDTO.UpdateReq req, Event event) {
        event.update(
                req.title(),
                req.content(),
                req.startTime(),
                req.endTime(),
                req.location(),
                req.address(),
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
            Event event,
            Member member,
            LocalDateTime start) {
        RecurrenceGroupSpec rgSpec = RecurrenceGroupConverter.from(eventReq, rgReq, null, start);

        AdjustedTime adjusted = RecurrenceTimeAdjuster.adjust(event.getStartTime(), event.getEndTime(), rgSpec);

        // 생성된 반복에 따른 일정 start,endTime 업데이트
        event.updateTime(adjusted.start(), adjusted.end());

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

        byte[] rgHash = SuggestionKeyUtil.rgHash(rg.getId());

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
            log.info("exception updated");
            InvalidationPlan invalidationPlan = suggestionInvalidationPlanner.planForSingleTarget(
                    SuggestionInvalidateReason.EXCEPTION_UPDATED,
                    rgHash
            );

            suggestionInvalidationDispatcher.dispatch(member.getId(), invalidationPlan);
            return;
        }

        RecurrenceException ex = RecurrenceGroupConverter.toRecurrenceExceptionForUpdate(
                req, rg, occurrenceDate, event.getDurationMinutes());
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

        log.info("exception updated");
        InvalidationPlan invalidationPlan = suggestionInvalidationPlanner.planForSingleTarget(
                SuggestionInvalidateReason.EXCEPTION_UPDATED,
                rgHash
        );

        suggestionInvalidationDispatcher.dispatch(member.getId(), invalidationPlan);
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
    private Event updateThisAndFutureEvents(
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
        return newEvent;
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

        upsertEventTitleHistory(eventSpec.title(), member.getId());
        upsertEventLocationHistory(eventSpec.location(), member.getId());

        return newEvent;
    }

    private void upsertEventTitleHistory(String title, Long memberId) {
        if (title == null) return;
        String trimmedTitle = title.trim();
        EventTitleHistory history =
                eventTitleHistoryRepository.findByMemberIdAndTitle(memberId, trimmedTitle)
                        .orElse(null);
        if (history == null) {
            history = EventHistoryConverter.toEventTitleHistory(memberId, trimmedTitle);
            eventTitleHistoryRepository.save(history);
        } else {
            history.updateLastUsedAt();
        }
    }

    private void upsertEventLocationHistory(String location, Long memberId) {
        if (location == null) return;
        String trimmedLocation = location.trim();
        EventLocationHistory history =
                eventLocationHistoryRepository.findByMemberIdAndLocation(memberId, trimmedLocation)
                        .orElse(null);
        if (history == null) {
            history = EventHistoryConverter.toEventLocationHistory(memberId, trimmedLocation);
            eventLocationHistoryRepository.save(history);
        } else {
            history.updateLastUsedAt();
        }
    }

    private boolean hasAnyEventFieldProvided(EventReqDTO.UpdateReq req) {
        return req.title() != null
                || req.content() != null
                || req.startTime() != null
                || req.endTime() != null
                || req.location() != null
                || req.address() != null
                || req.color() != null
                || req.isAllDay() != null;
    }

    private boolean hasAnyRecurrenceGroupFieldProvided(RecurrenceGroupReqDTO.UpdateReq req) {
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

    private boolean hasEventChanged(
            Event event,
            EventReqDTO.UpdateReq req,
            LocalDateTime start,
            LocalDateTime end,
            LocalDateTime occurrenceDate
            ) {
        boolean changed = false;

        if (event.getRecurrenceGroup() != null) {
            Optional<RecurrenceException> re =
                    recurrenceExRepository.findByRecurrenceGroupIdAndExceptionDateAndExceptionType(
                            event.getRecurrenceGroup().getId(), occurrenceDate, ExceptionType.OVERRIDE
                    );

            if (re.isPresent()) {
                RecurrenceException ex = re.get();
                if (req.title() != null) {
                    String baseTitle = ex.getTitle() != null ? ex.getTitle() : event.getTitle();
                    changed |= !Objects.equals(req.title(), baseTitle);
                }
                if (req.content() != null) {
                    String baseContent = ex.getContent() != null ? ex.getContent() : event.getContent();
                    changed |= !Objects.equals(req.content(), baseContent);
                }
                if (req.location() != null) {
                    String baseLocation = ex.getLocation() != null ? ex.getLocation() : event.getLocation();
                    changed |= !Objects.equals(req.location(), baseLocation);
                }
                if (req.address() != null) {
                    String baseAddress = ex.getAddress() != null ? ex.getAddress() : event.getAddress();
                    changed |= !Objects.equals(req.address(), baseAddress);
                }
                if (req.color() != null) {
                    EventColor baseColor = ex.getColor() != null ? ex.getColor() : event.getColor();
                    changed |= req.color() != baseColor;
                }
                if (req.isAllDay() != null) {
                    Boolean baseIsAllDay = ex.getIsAllDay() != null ? ex.getIsAllDay() : event.getIsAllDay();
                    changed |= !Objects.equals(req.isAllDay(), baseIsAllDay);
                }
                if (req.startTime() != null) {
                    LocalDateTime baseStart = ex.getStartTime() != null ? ex.getStartTime() : occurrenceDate;
                    changed |= !start.equals(baseStart);
                }
                if (req.endTime() != null) {
                    LocalDateTime baseEnd = ex.getEndTime() != null
                            ? ex.getEndTime() : occurrenceDate.plusMinutes(event.getDurationMinutes());
                    changed |= !end.equals(baseEnd);
                }

                return changed;
            }
        }

        if (req.title() != null) changed |= !Objects.equals(req.title(), event.getTitle());
        if (req.content() != null) changed |= !Objects.equals(req.content(), event.getContent());
        if (req.location() != null) changed |= !Objects.equals(req.location(), event.getLocation());
        if (req.address() != null) changed |= !Objects.equals(req.address(), event.getAddress());
        if (req.color() != null) changed |= req.color() != event.getColor();
        if (req.isAllDay() != null) changed |= !Objects.equals(req.isAllDay(), event.getIsAllDay());
        if (req.startTime() != null) changed |= !start.equals(occurrenceDate);
        if (req.endTime() != null) changed |= !end.equals(occurrenceDate.plusMinutes(event.getDurationMinutes()));

        return changed;
    }

    private boolean hasEventRecurrenceChanged(RecurrenceGroup rg, RecurrenceGroupReqDTO.UpdateReq req) {
        // 단일 일정 -> 반복 일정으로 수정하는 경우
        if (rg == null && req != null) return true;

        // 단일 일정 -> 단일 일정 / 반복 일정 -> 반복 일정
        if (rg == null || req == null) return false;

        boolean changed = false;

        if (req.frequency() != null) changed |= !Objects.equals(req.frequency(), rg.getFrequency());

        if (req.endType() != null) changed |= !Objects.equals(req.endType(), rg.getEndType());

        if (req.endDate() != null) changed |= !Objects.equals(req.endDate(), rg.getEndDate());

        if (req.occurrenceCount() != null) changed |= !req.occurrenceCount().equals(rg.getOccurrenceCount());

        if (req.monthlyType() != null) changed |= !Objects.equals(req.monthlyType(), rg.getMonthlyType());

        if (req.weekOfMonth() != null) changed |= !req.weekOfMonth().equals(rg.getWeekOfMonth());

        if (req.monthOfYear() != null) changed |= !req.monthOfYear().equals(rg.getMonthOfYear());

        if (req.daysOfWeek() != null) {
            String normalized = req.daysOfWeek().stream()
                    .sorted()                 // DayOfWeek는 MONDAY..SUNDAY 순서로 정렬됨
                    .map(DayOfWeek::name)     // "MONDAY"
                    .collect(Collectors.joining(",")); // 구분자 규칙 고정 (공백 X)

            changed |= !Objects.equals(normalized, rg.getDaysOfWeek());
        }

        if (req.daysOfMonth() != null) {
            String normalized = req.daysOfMonth().stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            changed |= !Objects.equals(normalized, rg.getDaysOfMonth());
        }

        if (req.weekdayRule() != null) {
            // SINGLE만 보냈고 dayOfWeekInMonth가 없으면 '수정 안 함'으로 간주 → 비교 자체를 안 함
            if (req.weekdayRule() == MonthlyWeekdayRule.SINGLE && req.dayOfWeekInMonth() == null) {
                // do nothing
            } else {
                List<DayOfWeek> daysOfWeek = RecurrenceUtils.parseDaysOfWeek(rg.getDayOfWeekInMonth());
                log.info("weekdayRule: {}, daysOfWeek: {}", req.weekdayRule(), daysOfWeek);
                changed |= req.weekdayRule() != RecurrenceUtils.inferWeekdayRule(daysOfWeek);
            }
        }

        if (req.dayOfWeekInMonth() != null) {
            String normalized = req.dayOfWeekInMonth().name();

            changed |= !Objects.equals(normalized, rg.getDayOfWeekInMonth());
        }

        if (req.intervalValue() != null) changed |= !req.intervalValue().equals(rg.getIntervalValue());

        return changed;
    }


    private LocalDateTime calStartTime(EventReqDTO.UpdateReq req, Event event, LocalDateTime occurrenceDate) {
        if (req.startTime() != null) {
            return req.startTime();
        }

        if (event.isRecurring()) {
            Optional<RecurrenceException> re = recurrenceExRepository.
                    findByRecurrenceGroupIdAndExceptionDateAndExceptionType(
                            event.getRecurrenceGroup().getId(), occurrenceDate, ExceptionType.OVERRIDE
                    );
            if (re.isPresent() && re.get().getStartTime() != null) {
                return re.get().getStartTime();
            }
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
