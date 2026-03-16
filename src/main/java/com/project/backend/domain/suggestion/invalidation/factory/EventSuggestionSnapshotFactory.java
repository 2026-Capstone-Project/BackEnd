package com.project.backend.domain.suggestion.invalidation.factory;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.suggestion.invalidation.snapshot.EventSuggestionSnapshot;
import com.project.backend.domain.suggestion.invalidation.publisher.SuggestionInvalidatePublisher;
import com.project.backend.domain.suggestion.invalidation.fingerprint.EventFingerPrint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.RecurrenceGroupFingerPrint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventSuggestionSnapshotFactory {

    private final SuggestionInvalidatePublisher suggestionInvalidatePublisher;

    public EventSuggestionSnapshot from(Event event) {
        byte[] eventHash = suggestionInvalidatePublisher.eventHash(
                event.getTitle(),
                event.getLocation()
        );
        EventFingerPrint eventFingerPrint = EventFingerPrint.from(event);

        byte[] recurrenceGroupHash = null;
        RecurrenceGroupFingerPrint recurrenceGroupFingerPrint = null;

        RecurrenceGroup recurrenceGroup = event.getRecurrenceGroup();
        if (recurrenceGroup != null) {
            recurrenceGroupHash = suggestionInvalidatePublisher.rgHash(recurrenceGroup.getId());
            recurrenceGroupFingerPrint = RecurrenceGroupFingerPrint.from(recurrenceGroup);
        }

        return new EventSuggestionSnapshot(
                eventHash,
                eventFingerPrint,
                recurrenceGroupHash,
                recurrenceGroupFingerPrint
        );
    }
}
