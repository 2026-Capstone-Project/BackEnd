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
import com.project.backend.domain.reminder.entity.ReminderSource;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.enums.ReminderRole;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.reminder.exception.ReminderErrorCode;
import com.project.backend.domain.reminder.exception.ReminderException;
import com.project.backend.domain.reminder.provider.ReminderSourceProvider;
import com.project.backend.domain.reminder.repository.ReminderRepository;
import com.project.backend.domain.reminder.provider.OccurrenceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final ReminderSourceProvider reminderSourceProvider;

    @Override
    public void createReminder(ReminderSource source, Long memberId) {
        LocalDateTime now = LocalDateTime.now();

        // 단일 일정 / 단일 할일
        if (!source.getIsRecurring()) {
            if (!source.getOccurrenceTime().isBefore(now)) {
                saveReminder(source, memberId);
            }
            return;
        }

        // 반복이 포함된 일정
        if (source.getTargetType() == TargetType.EVENT && source.getIsRecurring()) {
            if (!source.getOccurrenceTime().isAfter(now)) {
                // last는 현재보다 이후에 있는 계산된 일정의 startTime이거나,
                // 반복이 현재시간보다 이전에 종료되어 현재 시간보다 이전인 계산된 일정의 startTime
                // (반복을 통한 마지막으로 계산된 일정의 startTime)
                LocalDateTime last = eventQueryService.findNextOccurrenceAfterNow(source.getTargetId());
                // 현재시간보다 이전 날짜로 생성되었는데 반복을 통해 계산된 일정의 시간이 현재 시간보다 이후에 있는 경우
                if (!last.isBefore(now)) {
                    ReminderSource overrideRs = reminderSourceProvider.getEventReminderSourceWithTime(source, last);
                    saveReminder(overrideRs, memberId);
                    return;
                }
            }
            saveReminder(source, memberId);
        }
        if (source.getTargetType() == TargetType.TODO) {
            // 투두
        }
    }

    @Override
    public void createSingleOverrideReminder(Long eventId, Long memberId, LocalDateTime occurrenceTime, String title) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        log.info("2222222");
        ReminderSource source = ReminderConverter.toEventReminderSource(
                eventId,
                title,
                occurrenceTime,
                false
        );

        log.info("3333333");
        Reminder reminder = ReminderConverter.toReminderWithOccurrence(
                source,
                member,
                occurrenceTime,
                LifecycleStatus.ACTIVE,
                ReminderRole.OVERRIDE
        );

        log.info("4444444");
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
    public void refreshDueToUpdate(Reminder reminder) {
        doRefresh(reminder);
    }

    @Override
    public void refreshIfOccurrenceInvalidated(ReminderSource rs, Long exceptionId) {
        Reminder reminder = reminderRepository.findByIdAndTypeAndRole(
                rs.getTargetId(), rs.getTargetType(), ReminderRole.BASE)
                .orElseThrow(() -> new ReminderException(ReminderErrorCode.REMINDER_NOT_FOUND));

        RecurrenceException ex = recurrenceExRepository.findById(exceptionId)
                .orElseThrow(() ->
                        new RecurrenceGroupException(RecurrenceGroupErrorCode.RECURRENCE_EXCEPTION_NOT_FOUND));

        // 날짜가 바뀐 경우 -> 반복 + 일정에서 유일하게 수정된 일정에 대한 리마인더 생성
        if (ex.getStartTime() != null && !ex.getStartTime().toLocalDate().equals(ex.getExceptionDate())) {
            log.info("11111111");
            createSingleOverrideReminder(
                    rs.getTargetId(),
                    reminder.getMember().getId(),
                    ex.getStartTime(),
                    ex.getTitle() != null
                            ? ex.getTitle()
                            : reminder.getTitle()
            );
        }

        // 리마인더가 지정하고 있던 계산된 일정의 시간을 변경/삭제한 경우
        if (reminder.getOccurrenceTime().toLocalDate().equals(ex.getExceptionDate())) {
            refreshDueToUpdate(reminder);
        }
    }

    @Override
    public void updateReminderOfSingle(ReminderSource rs, Long memberId) {
        // 기존 리마인더 삭제
        deleteReminder(rs);

        saveReminder(rs, memberId);
    }

    @Override
    public void updateReminderOfRecurrence(ReminderSource rs, Long memberId, LocalDateTime occurrenceTime) {
        Member member= memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        Reminder reminder =
                ReminderConverter.toReminderWithOccurrence(
                        rs, member, occurrenceTime, LifecycleStatus.ACTIVE, ReminderRole.BASE);
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
    public void deleteReminderOfSingle(ReminderSource rs) {
        deleteReminder(rs);
    }

    @Override
    public void deleteReminderOfThisAndFollowings(ReminderSource rs, LocalDate occurrenceTime) {
        Optional<Reminder> reminder = reminderRepository.findByIdAndTypeAndRole(
                rs.getTargetId(), rs.getTargetType(), ReminderRole.BASE);

        if (reminder.isEmpty()) return;

        Reminder baseReminder = reminder.get();

        LocalDateTime targetTime = occurrenceTime.atTime(baseReminder.getOccurrenceTime().toLocalTime());

        // 수정한 일정에 대한 리마인더의 발생 시간이 수정한 일정의 시간보다 이전 or 동일하다면 리마인더 삭제
        if (!baseReminder.getOccurrenceTime().isAfter(targetTime)) {
            deleteReminderAfterOccurrenceTime(rs, targetTime);
        }
    }

    @Override
    public void deleteReminderOfAll(ReminderSource rs, LocalDateTime occurrenceTime) {
        deleteReminderAfterOccurrenceTime(rs, occurrenceTime);
    }

    private void saveReminder(ReminderSource rs, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 당장 활성화된 새 리마인더 생성
        Reminder reminder = ReminderConverter.toReminder(rs, member, LifecycleStatus.ACTIVE);
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

    private void deleteReminder(ReminderSource rs) {
        reminderRepository.deleteByTargetIdAndTargetType(rs.getTargetId(), rs.getTargetType());
    }

    private void deleteReminderAfterOccurrenceTime(ReminderSource rs, LocalDateTime occurrenceTime) {
        reminderRepository.deleteByTargetIDAndTargetTypeAndOccurrenceTime
                (rs.getTargetId(), rs.getTargetType(), occurrenceTime);
    }
}
