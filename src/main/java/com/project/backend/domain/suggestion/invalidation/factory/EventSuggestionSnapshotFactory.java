package com.project.backend.domain.suggestion.invalidation.factory;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.suggestion.invalidation.snapshot.EventSuggestionSnapshot;
import com.project.backend.domain.suggestion.invalidation.fingerprint.EventFingerPrint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.RecurrenceGroupFingerPrint;
import com.project.backend.domain.suggestion.util.SuggestionKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventSuggestionSnapshotFactory {

    public EventSuggestionSnapshot from(Event event) {
        byte[] eventHash = SuggestionKeyUtil.eventHash(
                event.getTitle(),
                event.getLocation(),
                event.getAddress()
        );
        EventFingerPrint eventFingerPrint = EventFingerPrint.from(event);

        byte[] recurrenceGroupHash = null;
        RecurrenceGroupFingerPrint recurrenceGroupFingerPrint = null;

        RecurrenceGroup recurrenceGroup = event.getRecurrenceGroup();
        if (recurrenceGroup != null) {
            recurrenceGroupHash = SuggestionKeyUtil.rgHash(recurrenceGroup.getId());
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
