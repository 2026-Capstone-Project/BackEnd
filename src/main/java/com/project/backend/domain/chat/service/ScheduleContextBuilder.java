package com.project.backend.domain.chat.service;

import com.project.backend.domain.chat.dto.DateRange;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ScheduleContextBuilder {

    private final EventRepository eventRepository;
    private final TodoRepository todoRepository;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    public String build(Long memberId, DateRange range) {
        List<Event> events = eventRepository.findByMemberIdAndStartTimeBetween(
                memberId, range.start(), range.end());

        List<Todo> todos = todoRepository.findByMemberIdAndStartDateBetween(
                        memberId, range.start().toLocalDate(), range.end().toLocalDate())
                .stream()
                .filter(t -> !Boolean.TRUE.equals(t.getIsCompleted())) // isCompleted=true 필터링 제거
                .toList();

        if (events.isEmpty() && todos.isEmpty()) {
            return "해당 기간에 일정과 할 일이 없습니다.";
        }

        StringBuilder sb = new StringBuilder();

        if (!events.isEmpty()) {
            sb.append("[일정]\n");
            events.stream()
                    .sorted(Comparator.comparing(Event::getStartTime))
                    .forEach(e -> sb.append(formatEvent(e)).append("\n"));
        }

        if (!todos.isEmpty()) {
            sb.append("[할 일]\n");
            todos.stream()
                    .sorted(Comparator.comparing(Todo::getStartDate))
                    .forEach(t -> sb.append(formatTodo(t)).append("\n"));
        }

        return sb.toString().trim();
    }

    private String formatEvent(Event event) {
        String date = event.getStartTime().format(DATE_FMT);
        String title = event.getTitle();

        if (Boolean.TRUE.equals(event.getIsAllDay())) {
            return String.format("- %s: %s (종일)", date, title);
        }

        String time = event.getStartTime().format(TIME_FMT) + "~" + event.getEndTime().format(TIME_FMT);
        String location = event.getLocation() != null ? " @" + event.getLocation() : "";
        return String.format("- %s %s: %s%s", date, time, title, location);
    }

    private String formatTodo(Todo todo) {
        String date = todo.getStartDate().format(DATE_FMT);
        String title = todo.getTitle();
        String dueTime = todo.getDueTime() != null
                ? " (" + todo.getDueTime().format(DateTimeFormatter.ofPattern("HH:mm")) + "까지)"
                : "";
        return String.format("• %s: %s%s", date, title, dueTime);
    }
}
