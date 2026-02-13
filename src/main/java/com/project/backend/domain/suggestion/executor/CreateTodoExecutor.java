package com.project.backend.domain.suggestion.executor;

import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 단발성 Todo Suggestion인 경우
 */
@Component
@AllArgsConstructor
public class CreateTodoExecutor implements SuggestionExecutor {

    private final TodoRepository todoRepository;

    @Override
    public boolean supports(Category category) {
        return false;
    }

    @Override
    public void execute(Suggestion suggestion, Status currentStatus) {

        Todo previousTodo = suggestion.getPreviousTodo();

        LocalDate startDate = currentStatus == Status.PRIMARY
                ? suggestion.getPrimaryAnchorDate()
                : suggestion.getSecondaryAnchorDate();

        Todo createdTodo = Todo.builder()
                .title(previousTodo.getTitle())
                .startDate(startDate)
                .dueTime(previousTodo.getDueTime())
                .isAllDay(previousTodo.getIsAllDay())
                .priority(previousTodo.getPriority())
                .memo(previousTodo.getMemo())
                .isCompleted(false)
                .sourceSuggestionId(suggestion.getId())
                .member(previousTodo.getMember())
                .todoRecurrenceGroup(previousTodo.getTodoRecurrenceGroup())
                .build();

        todoRepository.save(createdTodo);
    }
}
