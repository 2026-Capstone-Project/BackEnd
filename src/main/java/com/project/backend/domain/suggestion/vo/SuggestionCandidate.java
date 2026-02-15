package com.project.backend.domain.suggestion.vo;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.enums.EventColor;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.enums.Priority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SuggestionCandidate(
        // 공통
        Long id,
        String title,
        String content, // TODO : todo 에서는 메모
        LocalDateTime start,
        Boolean isAllDay,
        List<LocalDate> primaryAnchorDate,
        List<LocalDate> secondaryAnchorDate,
        Category category,

        // Event
        EventInfo eventInfo,

        // Todo
        TodoInfo todoInfo
) {
    public static SuggestionCandidate from(Event event) {
        return new SuggestionCandidate(
                event.getId(),
                event.getTitle(),
                event.getLocation(),
                event.getStartTime(),
                event.getIsAllDay(),
                null,
                null,
                Category.EVENT,
                new EventInfo(
                        event.getEndTime(),
                        event.getDurationMinutes(),
                        event.getColor()),
                null
        );
    }

    public static SuggestionCandidate from(Todo todo) {
        return new SuggestionCandidate(
                todo.getId(),
                todo.getTitle(),
                todo.getMemo(),
                todo.getStartDate().atTime(todo.getDueTime()),
                todo.getIsAllDay(),
                null,
                null,
                Category.TODO,
                null,
                new TodoInfo(todo.getPriority())
        );
    }

    public SuggestionCandidate withAnchor(List<LocalDate> primaryAnchor, List<LocalDate> secondaryAnchor) {
        return new SuggestionCandidate(
                this.id,
                this.title,
                this.content,
                this.start,
                this.isAllDay,
                primaryAnchor,
                secondaryAnchor,
                this.category,
                this.eventInfo,
                this.todoInfo
        );
    }

    public record EventInfo(
            LocalDateTime end,
            Integer durationMinutes,
            EventColor color
    ) {
    }

    public record TodoInfo(
            Priority priority
    ) {
    }
}
