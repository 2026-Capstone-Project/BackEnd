package com.project.backend.domain.reminder.handler;

import com.project.backend.domain.event.dto.EventChanged;
import com.project.backend.domain.event.service.query.EventQueryService;
import com.project.backend.domain.reminder.entity.ReminderSource;
import com.project.backend.domain.reminder.provider.ReminderSourceProvider;
import com.project.backend.domain.reminder.service.command.ReminderCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class EventReminderHandler {

    private final EventQueryService eventQueryService;
    private final ReminderCommandService reminderCommandService;
    private final ReminderSourceProvider reminderSourceProvider;

    @Transactional
    public void handle(EventChanged ec) {
        ReminderSource rs = reminderSourceProvider
                .getEventReminderSource(ec.eventId(), ec.title(), ec.occurrenceTime(), ec.isrRecurring());
        switch (ec.changeType()){
            case CREATED -> reminderCommandService.createReminder(rs, ec.memberId());
            case UPDATE_SINGLE -> {
                LocalDateTime now = LocalDateTime.now();
                if (!ec.occurrenceTime().isBefore(now)) {
                    reminderCommandService.updateReminderOfSingle(rs, ec.memberId());
                }
            }
            case UPDATE_ADD_RECURRENCE -> {
                LocalDateTime now = LocalDateTime.now();
                // 이미 지난 일정에 반복을 추가할 경우
                if (ec.occurrenceTime().isBefore(now)) {
                    // 해당 일정의 반복을 진행했을 때, 현재 시간과 같거나 이후의 계산된 시간이 나온다면 리마인더의 발생 시간 업데이트
                    // 현재 시간 이전 시간이 나온다면, 반복을 추가해도 더 이상 리마인더가 필요없기 때문에 리마인더 생성 x
                    LocalDateTime last = eventQueryService.findNextOccurrenceAfterNow(ec.eventId());
                    if (!last.isBefore(now)) {
                        reminderCommandService.updateReminderOfRecurrence(rs, ec.memberId(), last);
                    }
                }
            }
            case DELETED_SINGLE -> {
                reminderCommandService.deleteReminderOfSingle(rs);
            }
            case DELETED_THIS_AND_FOLLOWING ->
                    reminderCommandService.deleteReminderOfThisAndFollowings(rs, ec.occurrenceDate());
            case DELETED_ALL ->
                reminderCommandService.deleteReminderOfAll(rs, rs.getOccurrenceTime());
        }
    }
}
