package com.project.backend.domain.reminder.listener;

import com.project.backend.domain.event.dto.PlanChanged;
import com.project.backend.domain.event.dto.RecurrenceExceptionChanged;
import com.project.backend.domain.reminder.dto.ReminderDeleted;
import com.project.backend.domain.reminder.handler.PlanReminderHandler;
import com.project.backend.domain.reminder.handler.ExceptionReminderHandler;
import com.project.backend.domain.reminder.handler.ReminderDeletedHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderListener {

    private final PlanReminderHandler planReminderHandler;
    private final ExceptionReminderHandler exceptionReminderHandler;
    private final ReminderDeletedHandler reminderDeletedHandler;

    // 리스너를 호출하는 로직이 flush + commit 된 이후 실행
    // 일정 생성, 단일 일정 수정, 단일 일정에서 반복일정으로 수정하는 경우
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onPlanChanged(PlanChanged pc) { planReminderHandler.handle(pc); }

    // 반복이 있는 일정 수정,삭제 시, 그 반복에서 해당 일정만 수정/삭제 하는 경우
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onRecurrenceExceptionChanged(RecurrenceExceptionChanged rec) {
        exceptionReminderHandler.handle(rec);
    }

    // 수정된 일정에 대해 THIS_AND_FOLLWING 으로 재수정하거나 삭제 하는 경우
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onReminderDeleted(ReminderDeleted rd) {
        reminderDeletedHandler.handle(rd);
    }
}
