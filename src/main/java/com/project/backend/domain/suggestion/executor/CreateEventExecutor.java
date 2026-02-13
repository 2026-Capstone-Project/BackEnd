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
    public void execute(Suggestion suggestion, Status currentStatus) {
        Event previousEvent = suggestion.getPreviousEvent();

        LocalDate startDate = currentStatus == Status.PRIMARY
                ? suggestion.getPrimaryAnchorDate()
                : suggestion.getSecondaryAnchorDate();

        LocalTime prevStartTime = previousEvent.getStartTime().toLocalTime();

        LocalDateTime nextStart = startDate.atTime(prevStartTime);
        LocalDateTime nextEnd = nextStart.plusMinutes(previousEvent.getDurationMinutes());

        Event createdEvent = Event.builder()
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
                .build();

        eventRepository.save(createdEvent);
    }
}
