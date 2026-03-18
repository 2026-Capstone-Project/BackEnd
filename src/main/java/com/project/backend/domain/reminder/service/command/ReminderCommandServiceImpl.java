package com.project.backend.domain.reminder.service.command;

import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.exception.RecurrenceGroupErrorCode;
import com.project.backend.domain.event.exception.RecurrenceGroupException;
import com.project.backend.domain.event.repository.RecurrenceExceptionRepository;
import com.project.backend.domain.event.service.query.EventQueryService;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.reminder.converter.ReminderConverter;
import com.project.backend.domain.occurrence.dto.NextOccurrenceResult;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.dto.ReminderSource;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.enums.ReminderRole;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.reminder.exception.ReminderErrorCode;
import com.project.backend.domain.reminder.exception.ReminderException;
import com.project.backend.domain.reminder.repository.ReminderRepository;
import com.project.backend.domain.occurrence.service.OccurrenceResolver;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceException;
import com.project.backend.domain.todo.exception.TodoErrorCode;
import com.project.backend.domain.todo.exception.TodoException;
import com.project.backend.domain.todo.repository.TodoRecurrenceExceptionRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import com.project.backend.domain.todo.service.query.TodoQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReminderCommandServiceImpl implements ReminderCommandService {

    private final ReminderRepository reminderRepository;
    private final MemberRepository memberRepository;
    private final RecurrenceExceptionRepository recurrenceExRepository;
    private final OccurrenceResolver occurrenceResolver;
    private final EventQueryService eventQueryService;
    private final TodoQueryService todoQueryService;
    private final TodoRecurrenceExceptionRepository todoRecurrenceExceptionRepository;
    private final TodoRepository todoRepository;

    @Override
    public void createReminder(ReminderSource rs, Long memberId) {
        resolveBaseReminderSource(rs)
                .ifPresent(overrideRs -> saveReminder(overrideRs, memberId, null, ReminderRole.BASE));
    }

    @Override
    public void refreshIfExpired(Reminder reminder) {
        LocalDateTime now = LocalDateTime.now();

        // 변경하려는 리마인더의 설정된 시간이 현재 시간보다 이후일 경우
        if (!reminder.getOccurrenceTime().isBefore(now)) {
            return; // 아직 갱신 시점 아님 30
        }

        doRefresh(reminder);
    }

    @Override
    public void refreshIfOccurrenceInvalidated(ReminderSource rs, Long exceptionId, Boolean isSkip) {
        LocalDateTime now = LocalDateTime.now();

        // 수정한 날짜가 현재보다 이전이면 리마인더 생성 x
        if (!rs.occurrenceTime().isAfter(now)) return;

        Reminder reminder = findBaseReminder(rs);

        switch (rs.targetType()) {
            case EVENT -> handleEventOccurrenceInvalidated(rs, exceptionId, isSkip, reminder);
            case TODO -> handleTodoOccurrenceInvalidated(rs, exceptionId, isSkip, reminder);
        }
    }

    @Override
    public void updateReminderOfSingle(ReminderSource rs, Long memberId) {
        // 현재보다 해당 일정의 startTime이 이전이라면 리마인더 생성 x
        if (rs.occurrenceTime().isBefore(LocalDateTime.now())) return;

        // 기존 단일 일정에 대한 리마인더 삭제
        reminderRepository.deleteByTargetIdAndTargetType(rs.targetId(), rs.targetType());

        saveReminder(rs, memberId, null, ReminderRole.BASE);
    }

    @Override
    public void updateReminderOfRecurrence(ReminderSource rs, Long memberId, LocalDateTime occurrenceTime) {
        LocalDateTime now = LocalDateTime.now();

        // 기존 단일 일정에 대한 리마인더 삭제
        reminderRepository.deleteByTargetIdAndTargetType(rs.targetId(), rs.targetType());

        // 이미 지난 일정에 반복을 추가할 경우
        if (rs.occurrenceTime().isBefore(now)) {
            LocalDateTime last = (rs.targetType() == TargetType.EVENT)
                    ? eventQueryService.findNextOccurrenceAfterNow(rs.targetId())
                    : todoQueryService.findNextOccurrenceAfterNow(rs.targetId());
            // 해당 일정의 반복을 진행했을 때, 현재 시간과 같거나 이후의 계산된 시간이 나온다면 리마인더의 발생 시간 업데이트
            // 현재 시간 이전 시간이 나온다면, 반복을 추가해도 더 이상 리마인더가 필요없기 때문에 리마인더 생성 x
            if (last == null || last.isBefore(now)) {
                return;
            }
        }

        saveReminder(rs, memberId, null, ReminderRole.BASE);
    }

    @Override
    public void syncReminderAfterExceptionUpdate(ReminderSource rs, Long exceptionId, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        // 수정한 일정/할 일의 startTime or ExceptionDate가 현재보다 이전인 경우 리마인더 생성 x
        if (rs.occurrenceTime().isBefore(now)) return;

        // 수정하려는 일정/할 일의 startTime이 현재보다 이후인 경우
        Optional<Reminder> reminder = reminderRepository.findByRecurrenceExceptionIdAndTargetType(
                exceptionId,
                rs.targetType()
        );

        if (reminder.isEmpty()) {
            // 이전 수정된 일정의 날짜가 현재보다 이후여서 리마인더가 삭제된 경우 리마인더 새로 생성
            saveReminder(rs, memberId, exceptionId, ReminderRole.OVERRIDE);
            return;
        }

        Reminder re = reminder.get();

        // rs의 title이 저장된 리마인더와 일치하지 않은경우 (제목이 업데이트 된 경우)
        if (rs.title() != null && !rs.title().equals(re.getTitle())) {
            re.updateTitle(rs.title());
        }

        // 시간
        if (!rs.occurrenceTime().equals(re.getOccurrenceTime())) {
            re.updateOccurrence(rs.occurrenceTime());
        }
    }

    @Override
    public void deleteReminderOfSingle(Long targetId, TargetType targetType, LocalDateTime occurrenceTime) {
        reminderRepository.deleteByTargetIdAndTargetTypeAndOccurrenceTime(targetId, targetType, occurrenceTime);
    }

    @Override
    public void deleteReminderOfThisAndFollowings(Long targetId, TargetType targetType, LocalDateTime occurrenceTime) {
        Optional<Reminder> reminder = reminderRepository.findByIdAndTypeAndRole(
                targetId, targetType, ReminderRole.BASE);

        if (reminder.isEmpty()) return;

        Reminder re = reminder.get();
        // 원본 일정에 대한 리마인더의 발생 시간이 수정을 통해 생성된 일정의 startTime과 동일하거나 이후라면 원본 일정에 대한 리마인더 삭제
        if (!re.getOccurrenceTime().isBefore(occurrenceTime)) {
            reminderRepository.deleteById(re.getId());
        }

        // THIS_AND_FOLLOWING: 기준 occurrenceTime(포함) 이후에 발생하는 리마인더만 삭제한다.
        reminderRepository.deleteRemindersFromOccurrenceTime(targetId, targetType, occurrenceTime);
    }

    @Override
    public void deleteReminderOfAll(Long targetId, TargetType targetType) {
        Optional<Reminder> reminder = reminderRepository.findByIdAndTypeAndRole(
                targetId, targetType, ReminderRole.BASE);

        if (reminder.isEmpty()) return;

        // base, override 리마인더 모두 삭제
        reminderRepository.deleteByTargetIdAndTargetType(targetId, targetType);
    }

    // =================================== private method ============================================

    /**
     * 리마인더 생성 예외 조건:
     * 1. 단일 일정/할 일: 시작 시간이 현재보다 과거인 경우
     * 2. 반복 일정/할 일: 마지막 항목의 시작 시간이 현재보다 과거인 경우
     */
    private Optional<ReminderSource> resolveBaseReminderSource(ReminderSource rs) {
        LocalDateTime now = LocalDateTime.now();

        // 단일 일정 / 단일 할일
        if (!rs.isRecurring()) {
            if (rs.occurrenceTime().isBefore(now)) {
                return Optional.empty();
            }
            return Optional.of(rs);
        }

        LocalDateTime occurrenceTime = rs.occurrenceTime();

        if (!occurrenceTime.isAfter(now)) {
            // last는 현재보다 이후에 있는 계산된 일정의 startTime이거나,
            // 반복이 현재시간보다 이전에 종료되어 현재시간보다 이전인 계산된 일정의 startTime(반복을 통한 마지막으로 계산된 일정의 startTime)
            LocalDateTime last = rs.targetType() == TargetType.EVENT
                    ? eventQueryService.findNextOccurrenceAfterNow(rs.targetId())
                    : todoQueryService.findNextOccurrenceAfterNow(rs.targetId());
            // 현재시간보다 이전 날짜로 생성되었는데 반복을 통해 계산된 일정의 시간이 현재 시간보다 이전에 있는 경우
            if (last.isBefore(now)) {
                return Optional.empty();
            }
            occurrenceTime = last;
        }

        return Optional.of(ReminderConverter.toReminderSource(rs, occurrenceTime));
    }


    /**
     * 생성된 일정 or 할 일에 대한 리마인더 저장
     */
    private void saveReminder(ReminderSource rs, Long memberId, Long exceptionId, ReminderRole role) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 당장 활성화된 새 리마인더 생성
        Reminder reminder = ReminderConverter.toReminder(rs, member, rs.occurrenceTime(), exceptionId, LifecycleStatus.ACTIVE, role);
        reminderRepository.save(reminder);
    }

    /**
     * 이 일정만 수정 or 이 할 일만 수정을 통해 생성된 EventException, TodoException에 대한 리마인더 생성
     */
    private void createSingleOverrideReminder(
            Long targetId,
            TargetType targetType,
            Long memberId,
            LocalDateTime occurrenceTime,
            String title,
            Long exceptionId
    ) {
        ReminderSource source = ReminderConverter.toReminderSource(
                targetId,
                targetType,
                title,
                occurrenceTime,
                false
        );

        saveReminder(source, memberId, exceptionId, ReminderRole.OVERRIDE);
    }

    /**
     * 현재 시간보다 Reminder에 저장된 occurrenceTime이 이전이면 현재 시간보다 가장 빠른 이후의 occurrenceTime으로 업데이트
     * */
    private void doRefresh(Reminder reminder) {
        LocalDateTime now = LocalDateTime.now();

        NextOccurrenceResult result = occurrenceResolver.getNextOccurrence(reminder);

        if (!result.hasNext()) {
            reminder.terminate();
            return;
        }

        if (result.nextTime().isBefore(now)) {
            reminder.inActive();
            return;
        }

        reminder.updateOccurrence(result.nextTime());
    }

    /**
     * Type Base Reminder 찾기
     * */
    private Reminder findBaseReminder(ReminderSource rs) {
        return reminderRepository.findByIdAndTypeAndRole(
                        rs.targetId(), rs.targetType(), ReminderRole.BASE)
                .orElseThrow(() -> new ReminderException(ReminderErrorCode.REMINDER_NOT_FOUND));
    }

    /**
     * 이 일정만 수정을 통해 생성된 Exception에 대한 리마인더 생성
     * +
     * 기존 반복 일정에 대한 리마인더에 대한 occurrenceTime 업데이트 진행
     * */
    private void handleEventOccurrenceInvalidated(
            ReminderSource rs,
            Long exceptionId,
            Boolean isSkip,
            Reminder reminder
    ) {
        RecurrenceException ex = recurrenceExRepository.findById(exceptionId)
                .orElseThrow(() ->
                        new RecurrenceGroupException(RecurrenceGroupErrorCode.RECURRENCE_EXCEPTION_NOT_FOUND));

        // 수정(날짜 변경)에서만: 일회성 override 리마인더 생성
        if (!isSkip && ex.getStartTime() != null && !ex.getStartTime().equals(ex.getExceptionDate())) {
            createSingleOverrideReminder(
                    rs.targetId(),
                    rs.targetType(),
                    reminder.getMember().getId(),
                    rs.occurrenceTime(),
                    rs.title(),
                    ex.getId()
            );
        }

        // 수정/삭제에서 : 리마인더가 지정하고 있던 계산된 일정의 시간을 변경/삭제한 경우
        if (reminder.getOccurrenceTime().equals(ex.getExceptionDate())) {
            doRefresh(reminder);
        }
    }

    /**
     * 이 할 일만 수정을 통해 생성된 Exception에 대한 리마인더 생성
     * +
     * 기존 반복 할 일에 대한 리마인더에 대한 occurrenceTime 업데이트 진행
     * */
    private void handleTodoOccurrenceInvalidated(
            ReminderSource rs,
            Long exceptionId,
            Boolean isSkip,
            Reminder reminder
    ) {
        Todo todo = todoRepository.findById(rs.targetId())
                .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));

        TodoRecurrenceException tex = todoRecurrenceExceptionRepository.findById(exceptionId)
                .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_RECURRENCE_EXCEPTION_NOT_FOUND));

        // 수정(날짜 변경)에서만: 일회성 override 리마인더 생성
        if (!isSkip && !todo.getDueTime().equals(rs.occurrenceTime().toLocalTime())) {
            createSingleOverrideReminder(
                    rs.targetId(),
                    rs.targetType(),
                    reminder.getMember().getId(),
                    rs.occurrenceTime(),
                    rs.title(),
                    tex.getId()
            );
        }

        // 수정/삭제에서 : 리마인더가 지정하고 있던 계산된 일정의 시간을 변경/삭제한 경우
        if (reminder.getOccurrenceTime().toLocalDate().equals(tex.getExceptionDate())) {
            doRefresh(reminder);
        }
    }
}
