package com.project.backend.domain.reminder.service.command;

import com.project.backend.domain.event.dto.RecurrenceEnded;
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
import com.project.backend.domain.reminder.dto.NextOccurrenceResult;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.reminder.dto.ReminderSource;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.enums.ReminderRole;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.reminder.exception.ReminderErrorCode;
import com.project.backend.domain.reminder.exception.ReminderException;
import com.project.backend.domain.reminder.repository.ReminderRepository;
import com.project.backend.domain.reminder.provider.OccurrenceProvider;
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
    private final OccurrenceProvider occurrenceProvider;
    private final EventQueryService eventQueryService;
    private final TodoQueryService todoQueryService;

    @Override
    public void createReminder(ReminderSource source, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        // 단일 일정 / 단일 할일
        if (!source.isRecurring()) {
            if (!source.occurrenceTime().isBefore(now)) {
                saveReminder(source, memberId, null, ReminderRole.BASE);
            }
            return;
        }

        // 반복이 포함된 일정
        if (source.targetType() == TargetType.EVENT) {
            if (!source.occurrenceTime().isAfter(now)) {
                // last는 현재보다 이후에 있는 계산된 일정의 startTime이거나,
                // 반복이 현재시간보다 이전에 종료되어 현재시간보다 이전인 계산된 일정의 startTime(반복을 통한 마지막으로 계산된 일정의 startTime)
                LocalDateTime last = eventQueryService.findNextOccurrenceAfterNow(source.targetId());
                // 현재시간보다 이전 날짜로 생성되었는데 반복을 통해 계산된 일정의 시간이 현재 시간보다 이후에 있는 경우
                if (!last.isBefore(now)) {
                    ReminderSource overrideRs = ReminderConverter.toReminderSource(source, last);
                    saveReminder(overrideRs, memberId, null, ReminderRole.BASE);
                    return;
                }
            }
            saveReminder(source, memberId, null, ReminderRole.BASE);
        }
        if (source.targetType() == TargetType.TODO) {
            // 투두
        }
    }

    @Override
    public void createSingleOverrideReminder(
            Long targetId,
            TargetType targetType,
            Long memberId,
            LocalDateTime occurrenceTime,
            String title,
            Long exceptionId
            ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        ReminderSource source = ReminderConverter.toReminderSource(
                targetId,
                targetType,
                title,
                occurrenceTime,
                false
        );

        Reminder reminder = ReminderConverter.toReminderWithOccurrence(
                source,
                member,
                occurrenceTime,
                LifecycleStatus.ACTIVE,
                ReminderRole.OVERRIDE,
                exceptionId
        );

        reminderRepository.save(reminder);
    }

    @Override
    public List<Reminder> findActiveReminders() {
        return reminderRepository.findAllByLifecycleStatus(LifecycleStatus.ACTIVE);
    }

    @Override
    public void refreshIfExpired(Reminder reminder) {
        LocalDateTime now = LocalDateTime.of(2026, 2, 18, 0, 0);

        // 변경하려는 리마인더의 설정된 시간이 현재 시간보다 이후일 경우
        if (!reminder.getOccurrenceTime().isBefore(now)) {
            return; // 아직 갱신 시점 아님 30
        }

        doRefresh(reminder);
    }

    @Override
    public void refreshIfOccurrenceInvalidated(ReminderSource rs, Long exceptionId, Boolean isSkip) {
        Reminder reminder = reminderRepository.findByIdAndTypeAndRole(
                rs.targetId(), rs.targetType(), ReminderRole.BASE)
                .orElseThrow(() -> new ReminderException(ReminderErrorCode.REMINDER_NOT_FOUND));

        RecurrenceException ex = recurrenceExRepository.findById(exceptionId)
                .orElseThrow(() ->
                        new RecurrenceGroupException(RecurrenceGroupErrorCode.RECURRENCE_EXCEPTION_NOT_FOUND));

        // // 수정(날짜 변경)에서만: 일회성 override 리마인더 생성
        if (!isSkip && ex.getStartTime() != null && !ex.getStartTime().equals(ex.getExceptionDate())) {
            createSingleOverrideReminder(
                    rs.targetId(),
                    rs.targetType(),
                    reminder.getMember().getId(),
                    ex.getStartTime(),
                    ex.getTitle() != null ? ex.getTitle() : reminder.getTitle(),
                    ex.getId()
            );
        }

        // 수정/삭제에서 : 리마인더가 지정하고 있던 계산된 일정의 시간을 변경/삭제한 경우
        if (reminder.getOccurrenceTime().equals(ex.getExceptionDate())) {
            doRefresh(reminder);
        }
    }

    @Override
    public void updateReminderOfSingle(ReminderSource rs, Long memberId) {
        // 현재보다 해당 일정의 startTime이 이전이라면 리마인더 생성 x
        if (rs.occurrenceTime().isBefore(LocalDateTime.now())) return;

        // 기존 리마인더 삭제
        deleteReminder(rs.targetId(), rs.targetType(), rs.occurrenceTime());

        saveReminder(rs, memberId, null, ReminderRole.BASE);
    }

    @Override
    public void updateReminderOfRecurrence(ReminderSource rs, Long memberId, LocalDateTime occurrenceTime) {
        LocalDateTime now = LocalDateTime.now();

        // 이미 지난 일정에 반복을 추가할 경우
        if (rs.occurrenceTime().isBefore(now)) {
            LocalDateTime last;
            if (rs.targetType() == TargetType.EVENT) {
                last = eventQueryService.findNextOccurrenceAfterNow(rs.targetId());
            } else {
                last = todoQueryService.findNextOccurrenceAfterNow(rs.targetId());
            }
            // 해당 일정의 반복을 진행했을 때, 현재 시간과 같거나 이후의 계산된 시간이 나온다면 리마인더의 발생 시간 업데이트
            // 현재 시간 이전 시간이 나온다면, 반복을 추가해도 더 이상 리마인더가 필요없기 때문에 리마인더 생성 x
            if (last.isBefore(now)) {
                return;
            }
        }
        Member member= memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Reminder reminder =
                ReminderConverter.toReminderWithOccurrence(
                        rs, member, occurrenceTime, LifecycleStatus.ACTIVE, ReminderRole.BASE, null);
        reminderRepository.save(reminder);
    }

    @Override
    public void cleanupBaseReminderOnUpdate(RecurrenceEnded re) {
        Reminder reminder = reminderRepository.findByIdAndTypeAndRole(
                re.targetId(), re.targetType(), ReminderRole.BASE
        ).orElseThrow(() -> new ReminderException(ReminderErrorCode.REMINDER_NOT_FOUND));

        // 원본 일정에 대한 리마인더의 발생 시간이 수정을 통해 생성된 일정의 startTime과 동일하거나 이후라면 원본 일정에 대한 리마인더 삭제
        if (!reminder.getOccurrenceTime().isBefore(re.startTime())) {
            reminderRepository.deleteById(reminder.getId());
        }
    }

    @Override
    public void syncReminderAfterExceptionUpdate(ReminderSource rs, Long exceptionId, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        // 수정한 일정의 startTime or ExceptionDate가 현재보다 이전인 경우 리마인더 생성 x
        if (rs.occurrenceTime().isBefore(now)) return;

        // 수정하려는 일정의 startTime이 현재보다 이후인 경우
        Optional<Reminder> reminder = reminderRepository.findByRecurrenceExceptionId(exceptionId);

        if (reminder.isEmpty()) {
            // 이전 수정된 일정의 날짜가 현재보다 이후여서 리마인더가 삭제된 경우 리마인더 새로 생성
            saveReminder(rs, memberId,exceptionId, ReminderRole.OVERRIDE);
            return;
        }

        Reminder re = reminder.get();

        // rs의 title이 저장된 리마인더와 일치하지 않은경우 (제목이 업데이트 된 경우)
        if (rs.title() != null && !rs.title().equals(re.getTitle())) {
            re.updateTitle(rs.title());
        }

        if (!rs.occurrenceTime().equals(re.getOccurrenceTime())) {
            re.updateOccurrence(rs.occurrenceTime());
        }
    }

    @Override
    public void deleteReminderOfSingle(Long targetId, TargetType targetType, LocalDateTime occurrenceTime) {
        deleteReminder(targetId, targetType, occurrenceTime);
    }

    @Override
    public void deleteReminderOfThisAndFollowings(Long targetId, TargetType targetType, LocalDateTime occurrenceTime) {
        Optional<Reminder> reminder = reminderRepository.findByIdAndTypeAndRole(
                targetId, targetType, ReminderRole.BASE);

        if (reminder.isEmpty()) return;

        // THIS_AND_FOLLOWING: 기준 occurrenceTime(포함) 이후에 발생하는 리마인더만 삭제한다.
        deleteReminderAfterOccurrenceTime(targetId, targetType, occurrenceTime);
    }

    @Override
    public void deleteReminderOfAll(Long targetId, TargetType targetType, LocalDateTime occurrenceTime) {
        deleteReminderAfterOccurrenceTime(targetId, targetType, occurrenceTime);
    }

    private void saveReminder(ReminderSource rs, Long memberId, Long exceptionId, ReminderRole role) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 당장 활성화된 새 리마인더 생성
        Reminder reminder = ReminderConverter.toReminder(rs, member, exceptionId, LifecycleStatus.ACTIVE, role);
        reminderRepository.save(reminder);
    }

    private void doRefresh(Reminder reminder) {
        LocalDateTime now = LocalDateTime.now();

        NextOccurrenceResult result = occurrenceProvider.getNextOccurrence(reminder);

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

    private void deleteReminder(Long targetId, TargetType targetType, LocalDateTime occurrenceTime) {
        reminderRepository.deleteByTargetIdAndTargetTypeAndOccurrenceTime(targetId, targetType, occurrenceTime);
    }

    private void deleteReminderAfterOccurrenceTime(
            Long targetId,
            TargetType targetType,
            LocalDateTime occurrenceTime
    ) {
        reminderRepository.deleteRemindersFromOccurrenceTime(targetId, targetType, occurrenceTime);
    }
}
