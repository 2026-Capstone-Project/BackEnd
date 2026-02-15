package com.project.backend.domain.suggestion.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TargetKeyHashUtil {
    public static byte[] sha256(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
