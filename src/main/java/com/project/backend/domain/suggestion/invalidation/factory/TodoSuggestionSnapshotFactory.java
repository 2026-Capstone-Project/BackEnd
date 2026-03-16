package com.project.backend.domain.suggestion.invalidation.factory;

import com.project.backend.domain.suggestion.invalidation.snapshot.TodoSuggestionSnapshot;
import com.project.backend.domain.suggestion.invalidation.publisher.SuggestionInvalidatePublisher;
import com.project.backend.domain.suggestion.invalidation.fingerprint.TodoFingerPrint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.TodoRecurrenceGroupFingerPrint;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TodoSuggestionSnapshotFactory {

    private final SuggestionInvalidatePublisher suggestionInvalidatePublisher;

    public TodoSuggestionSnapshot from(Todo todo) {
        byte[] todoHash = suggestionInvalidatePublisher.todoHash(
                todo.getTitle(),
                todo.getMemo()
        );
        TodoFingerPrint todoFingerPrint = TodoFingerPrint.from(todo);

        byte[] todoRecurrenceGroupHash = null;
        TodoRecurrenceGroupFingerPrint todoRecurrenceGroupFingerPrint = null;

        TodoRecurrenceGroup todoRecurrenceGroup = todo.getTodoRecurrenceGroup();
        if (todoRecurrenceGroup != null) {
            todoRecurrenceGroupHash = suggestionInvalidatePublisher.trgHash(todoRecurrenceGroup.getId());
            todoRecurrenceGroupFingerPrint = TodoRecurrenceGroupFingerPrint.from(todoRecurrenceGroup);
        }

        return new TodoSuggestionSnapshot(
                todoHash,
                todoFingerPrint,
                todoRecurrenceGroupHash,
                todoRecurrenceGroupFingerPrint
        );
    }
}
