package com.project.backend.domain.reminder.listener;

import com.project.backend.domain.event.dto.EventChanged;
import com.project.backend.domain.event.dto.RecurrenceEnded;
import com.project.backend.domain.event.dto.RecurrenceExceptionChanged;
import com.project.backend.domain.reminder.entity.ReminderSource;
import com.project.backend.domain.reminder.handler.EventReminderHandler;
import com.project.backend.domain.reminder.handler.ExceptionReminderHandler;
import com.project.backend.domain.reminder.handler.RecurrenceEndedHandler;
import com.project.backend.domain.reminder.provider.ReminderSourceProvider;
import com.project.backend.domain.reminder.service.command.ReminderCommandService;
import com.project.backend.domain.todo.dto.TodoChanged;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.exception.TodoErrorCode;
import com.project.backend.domain.todo.exception.TodoException;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderListener {

    private final ReminderCommandService reminderCommandService;
    private final ReminderSourceProvider reminderSourceProvider;

    private final EventReminderHandler eventReminderHandler;
    private final ExceptionReminderHandler exceptionReminderHandler;
    private final TodoRepository todoRepository;
    private final RecurrenceEndedHandler recurrenceEndedHandler;

    // 리스너를 호출하는 로직이 flush + commit 된 이후 실행
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onEvent(EventChanged ec) {
        eventReminderHandler.handle(ec);
    }

    // 반복이 있는 일정 수정,삭제 시, 그 반복에서 해당 일정만 수정/삭제 하는 경우
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onEvent(RecurrenceExceptionChanged rec) {
        exceptionReminderHandler.handle(rec);
    }

    // 반복이 있는 일정 수정 시, 해당 일정과 그 이후 일정들을 수정하는 경우
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onEvent(RecurrenceEnded re) {
        recurrenceEndedHandler.handle(re);
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onTodo(TodoChanged t) {
        Todo todo = todoRepository.findById(t.targetId())
                .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));
        ReminderSource rs = reminderSourceProvider.getTodoReminderSource(todo.getId());
        switch (t.changeType()){
            case CREATED -> reminderCommandService.createReminder(rs, todo.getMember().getId());
            //case UPDATED -> reminderCommandService.updateReminder();
            // case DELETED -> reminderCommandService.deleteReminder(rs);
        }
    }
}
