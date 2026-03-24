package com.project.backend.domain.suggestion.invalidation.snapshot;

import com.project.backend.domain.suggestion.invalidation.fingerprint.TodoFingerPrint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.TodoRecurrenceGroupFingerPrint;

/**
 * Todo 무효화 판단에 사용하는 snapshot
 */
public record TodoSuggestionSnapshot(
        byte[] planKeyHash,
        TodoFingerPrint planFingerprint,
        byte[] groupKeyHash,
        TodoRecurrenceGroupFingerPrint groupFingerprint
) implements SuggestionInvalidationSnapshot<TodoFingerPrint, TodoRecurrenceGroupFingerPrint> {
}