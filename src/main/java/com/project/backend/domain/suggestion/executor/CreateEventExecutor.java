package com.project.backend.domain.suggestion.executor;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 단발성 Event Suggestion인 경우
 */
@Component
@AllArgsConstructor
public class CreateEventExecutor implements SuggestionExecutor {

    private final EventRepository eventRepository;

    @Override
    public boolean supports(Category category) {
        return false;
    }

    @Override
    public void execute(Suggestion suggestion, Status currentStatus, Long memberId) {
        Event previousEvent = suggestion.getPreviousEvent();

        List<LocalDate> anchors = currentStatus == Status.PRIMARY
                ? suggestion.getPrimaryAnchorDate()
                : suggestion.getSecondaryAnchorDate();

        if (anchors == null || anchors.isEmpty()) {
            return;
        }
        List<Event> toSave = new ArrayList<>();
        for (LocalDate anchor : anchors) {
            LocalTime prevStartTime = previousEvent.getStartTime().toLocalTime();

            LocalDateTime nextStart = anchor.atTime(prevStartTime);
            LocalDateTime nextEnd = nextStart.plusMinutes(previousEvent.getDurationMinutes());

            boolean exist = eventRepository.existsByMemberIdAndTitleAndLocationAndStartTime(
                    memberId,
                    previousEvent.getTitle(),
                    previousEvent.getLocation(),
                    nextStart);

            if (exist) {
                System.out.println("이미존재");
                return;
            }

            toSave.add(Event.builder()
                    .title(previousEvent.getTitle())
                    .content(previousEvent.getContent())
                    .startTime(nextStart)
                    .endTime(nextEnd)
                    .location(previousEvent.getLocation())
                    .recurrenceFrequency(previousEvent.getRecurrenceFrequency())
                    .color(previousEvent.getColor())
                    .isAllDay(previousEvent.getIsAllDay())
                    .durationMinutes(previousEvent.getDurationMinutes())
                    .sourceSuggestionId(suggestion.getId()) // 멱등키
                    .member(previousEvent.getMember())
                    .recurrenceGroup(previousEvent.getRecurrenceGroup())
                    .build());
        }

        eventRepository.saveAll(toSave);
    }
}
