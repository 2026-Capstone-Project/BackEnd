package com.project.backend.domain.suggestion.executor;

import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    public void execute(Suggestion suggestion, Status currentStatus, Long memberId) {

        Todo previousTodo = suggestion.getPreviousTodo();

        List<LocalDate> anchors = currentStatus == Status.PRIMARY
                ? suggestion.getPrimaryAnchorDate()
                : suggestion.getSecondaryAnchorDate();

        if (anchors == null || anchors.isEmpty()) {
            return;
        }
        List<Todo> toSave = new ArrayList<>();
        for (LocalDate anchor : anchors) {
            boolean exist = todoRepository.existsByMemberIdAndTitleAndMemoAndStartDateAndDueTime(
                    memberId,
                    previousTodo.getTitle(),
                    previousTodo.getMemo(),
                    anchor,
                    previousTodo.getDueTime()
            );

            if (exist) {
                return; // anchor 중에 이미 생성된게 하나라도 있다면 전부 생성하지 않음 (같은 키 객체 생성시 애초에 선제적 제안이 만료됨)
//                continue; // anchor 중에 이미 생성된 것은 제외하고 나머지만 생성
            }

            toSave.add(Todo.builder()
                    .title(previousTodo.getTitle())
                    .startDate(anchor)
                    .dueTime(previousTodo.getDueTime())
                    .isAllDay(previousTodo.getIsAllDay())
                    .priority(previousTodo.getPriority())
                    .memo(previousTodo.getMemo())
                    .isCompleted(false)
                    .sourceSuggestionId(suggestion.getId())
                    .member(previousTodo.getMember())
                    .todoRecurrenceGroup(previousTodo.getTodoRecurrenceGroup())
                    .build());
        }


        todoRepository.saveAll(toSave);
    }
}
