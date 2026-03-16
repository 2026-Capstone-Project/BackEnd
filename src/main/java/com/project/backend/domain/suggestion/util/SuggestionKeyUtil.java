package com.project.backend.domain.suggestion.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SuggestionKeyUtil {

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }

    // 단발성: title+location+address
    public static String eventKey(String title, String location, String address) {
        return "E|" + norm(title) + "|" + norm(location) + "|" + norm(address);
    }

    public static String todoKey(String title, String memo) {
        return "T|" + norm(title) + "|" + norm(memo);
    }

    // 반복그룹: rgId
    public static String rgKey(Long rgId) {
        return "RG|" + rgId;
    }

    public static String trgKey(Long trgId) {
        return "TRG|" + trgId;
    }

    // 해시 유틸
    public static byte[] eventHash(String title, String location, String address) {
        return TargetKeyHashUtil.sha256(SuggestionKeyUtil.eventKey(title, location, address));
    }

    public static byte[] todoHash(String title, String memo) {
        return TargetKeyHashUtil.sha256(SuggestionKeyUtil.todoKey(title, memo));
    }

    public static byte[] rgHash(Long rgId) {
        return TargetKeyHashUtil.sha256(SuggestionKeyUtil.rgKey(rgId));
    }

    public static byte[] trgHash(Long trgId) {
        return TargetKeyHashUtil.sha256(SuggestionKeyUtil.trgKey(trgId));
    }
}
