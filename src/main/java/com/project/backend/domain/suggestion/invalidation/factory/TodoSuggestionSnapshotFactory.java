package com.project.backend.domain.suggestion.invalidation.factory;

import com.project.backend.domain.suggestion.invalidation.snapshot.TodoSuggestionSnapshot;
import com.project.backend.domain.suggestion.invalidation.fingerprint.TodoFingerPrint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.TodoRecurrenceGroupFingerPrint;
import com.project.backend.domain.suggestion.util.SuggestionKeyUtil;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Todo → Suggestion invalidation용 snapshot 생성
 */
@Component
@RequiredArgsConstructor
public class TodoSuggestionSnapshotFactory {

    public TodoSuggestionSnapshot from(Todo todo) {
        // Todo 기준 target hash
        byte[] todoHash = SuggestionKeyUtil.todoHash(
                todo.getTitle(),
                todo.getMemo()
        );

        // Todo 변경 감지용 fingerprint
        TodoFingerPrint todoFingerPrint = TodoFingerPrint.from(todo);

        byte[] todoRecurrenceGroupHash = null;
        TodoRecurrenceGroupFingerPrint todoRecurrenceGroupFingerPrint = null;

        // 반복 할 일이면 그룹 기준 hash + fingerprint도 포함
        TodoRecurrenceGroup trg = todo.getTodoRecurrenceGroup();
        if (trg != null) {
            todoRecurrenceGroupHash = SuggestionKeyUtil.trgHash(trg.getId());
            todoRecurrenceGroupFingerPrint = TodoRecurrenceGroupFingerPrint.from(trg);
        }

        return new TodoSuggestionSnapshot(
                todoHash,
                todoFingerPrint,
                todoRecurrenceGroupHash,
                todoRecurrenceGroupFingerPrint
        );
    }
}