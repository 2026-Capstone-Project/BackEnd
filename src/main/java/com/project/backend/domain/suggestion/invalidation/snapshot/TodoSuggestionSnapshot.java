package com.project.backend.domain.suggestion.invalidation.snapshot;

import com.project.backend.domain.suggestion.invalidation.fingerprint.TodoFingerPrint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.TodoRecurrenceGroupFingerPrint;

public record TodoSuggestionSnapshot(
        byte[] planKeyHash,
        TodoFingerPrint planFingerprint,
        byte[] groupKeyHash,
        TodoRecurrenceGroupFingerPrint groupFingerprint
) implements SuggestionInvalidationSnapshot<TodoFingerPrint, TodoRecurrenceGroupFingerPrint> {
}
