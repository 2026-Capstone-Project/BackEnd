package com.project.backend.domain.suggestion.invalidation.snapshot;

import com.project.backend.domain.suggestion.invalidation.fingerprint.EventFingerPrint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.RecurrenceGroupFingerPrint;

public record EventSuggestionSnapshot(
        byte[] planKeyHash,
        EventFingerPrint planFingerprint,
        byte[] groupKeyHash,
        RecurrenceGroupFingerPrint groupFingerprint
) implements SuggestionInvalidationSnapshot<EventFingerPrint, RecurrenceGroupFingerPrint> {
}
