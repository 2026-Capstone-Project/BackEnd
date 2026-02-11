package com.project.backend.domain.suggestion.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SuggestionTargetKeyUtil {

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }

    // 단발성: title+location
    public static String eventKey(String title, String location) {
        return "E|" + norm(title) + "|" + norm(location);
    }

    // 반복그룹: rgId
    public static String rgKey(Long rgId) {
        return "RG|" + rgId;
    }
}
