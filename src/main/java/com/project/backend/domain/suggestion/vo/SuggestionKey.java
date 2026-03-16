package com.project.backend.domain.suggestion.vo;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.todo.entity.Todo;

public record SuggestionKey(
        String title,
        // TODO : EventSuggestionKey, TodoSuggestionKey로 분리하기
        String identifier, // event의 경우 식별자를 장소, todo의 경우 식별자를 메모로 했음 (변경 가능)
        String address
) {
    public static SuggestionKey from(Event event) {
        return new SuggestionKey(
                normalize(event.getTitle()),
                normalize(event.getLocation()),
                normalize(event.getAddress())
        );
    }

    public static SuggestionKey from(Todo todo) {
        return new SuggestionKey(
                normalize(todo.getTitle()),
                normalize(todo.getMemo()),
                null
        );
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
