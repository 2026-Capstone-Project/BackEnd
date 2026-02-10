package com.project.backend.domain.reminder.provider;

import com.project.backend.domain.reminder.converter.ReminderConverter;
import com.project.backend.domain.reminder.entity.ReminderSource;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.exception.TodoErrorCode;
import com.project.backend.domain.todo.exception.TodoException;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReminderSourceProvider {

    private final TodoRepository todoRepository;

    public ReminderSource getEventReminderSource(
            Long eventId,
            String title,
            LocalDateTime occurrenceTime,
            Boolean isRecurring
            ) {
        return ReminderConverter.toEventReminderSource(eventId, title, occurrenceTime, isRecurring);
    }

    public ReminderSource getEventReminderSourceWithTime(
            ReminderSource base,
            LocalDateTime occurrenceTime
    ){
        return ReminderConverter.toEventReminderSource(
                base.getTargetId(),
                base.getTitle(),
                occurrenceTime,
                base.getIsRecurring()
        );
    }

    public ReminderSource getTodoReminderSource(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));
        return ReminderConverter.toTodoReminderSource(todo);
    }
}
