package com.project.backend.domain.suggestion.vo;

import com.project.backend.domain.event.entity.Event;

public record SuggestionKey(
        String title,
        String location
) {
    public static SuggestionKey from(Event event) {
        return new SuggestionKey(
                normalize(event.getTitle()),
                normalize(event.getLocation())
        );
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
