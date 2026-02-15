package com.project.backend.domain.suggestion.executor;

import com.project.backend.domain.suggestion.enums.SuggestionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 입력받은 Suggestion 객체의 SuggestionType에 따라 알맞은 SuggestionExecutor를 호출하는 Factory Class
 */
@Component
@AllArgsConstructor
public class SuggestionExecutorFactory {

    // TODO : Map 방식으로 변경하기
    private final CreateEventExecutor createEventExecutor;
    private final CreateTodoExecutor createTodoExecutor;
    private final CreateRecurrenceEventExecutor createRecurrenceEventExecutor;
    private final CreateRecurrenceTodoExecutor createRecurrenceTodoExecutor;

    public SuggestionExecutor getExecutor(SuggestionType suggestionType) {
        return switch (suggestionType) {
            case CREATE_EVENT -> createEventExecutor;
            case CREATE_TODO -> createTodoExecutor;
            case EXTEND_EVENT_RECURRENCE_GROUP ->  createRecurrenceEventExecutor;
            case EXTEND_TODO_RECURRENCE_GROUP -> createRecurrenceTodoExecutor;
        };
    }
}
