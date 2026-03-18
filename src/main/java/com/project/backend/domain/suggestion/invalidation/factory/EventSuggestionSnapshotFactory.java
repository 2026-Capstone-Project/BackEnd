package com.project.backend.domain.suggestion.invalidation.factory;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.suggestion.invalidation.snapshot.EventSuggestionSnapshot;
import com.project.backend.domain.suggestion.invalidation.fingerprint.EventFingerPrint;
import com.project.backend.domain.suggestion.invalidation.fingerprint.RecurrenceGroupFingerPrint;
import com.project.backend.domain.suggestion.util.SuggestionKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Event → Suggestion invalidation용 snapshot 생성
 * (hash + fingerprint 묶어서 반환)
 */
@Component
@RequiredArgsConstructor
public class EventSuggestionSnapshotFactory {

    public EventSuggestionSnapshot from(Event event) {

        // Event 기준 target hash (suggestion 식별용)
        byte[] eventHash = SuggestionKeyUtil.eventHash(
                event.getTitle(),
                event.getLocation(),
                event.getAddress()
        );

        // Event 변경 감지용 fingerprint
        EventFingerPrint eventFingerPrint = EventFingerPrint.from(event);

        byte[] recurrenceGroupHash = null;
        RecurrenceGroupFingerPrint recurrenceGroupFingerPrint = null;

        // 반복 일정이면 그룹 기준 hash + fingerprint도 포함
        RecurrenceGroup rg = event.getRecurrenceGroup();
        if (rg != null) {
            recurrenceGroupHash = SuggestionKeyUtil.rgHash(rg.getId());
            recurrenceGroupFingerPrint = RecurrenceGroupFingerPrint.from(rg);
        }

        return new EventSuggestionSnapshot(
                eventHash,
                eventFingerPrint,
                recurrenceGroupHash,
                recurrenceGroupFingerPrint
        );
    }
}
