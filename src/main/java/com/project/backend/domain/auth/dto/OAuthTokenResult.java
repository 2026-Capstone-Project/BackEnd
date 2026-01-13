package com.project.backend.domain.auth.dto;

import com.project.backend.domain.auth.enums.Provider;
import lombok.Builder;

/**
 * OAuth 토큰 발급 결과 통합 DTO
 * - Kakao, Naver: accessToken 사용
 * - Google: idToken 사용
 */
@Builder
public record OAuthTokenResult(
        String accessToken,
        String refreshToken,
        String idToken,
        Provider provider
) {
    public boolean hasAccessToken() {
        return accessToken != null && !accessToken.isEmpty();
    }

    public boolean hasIdToken() {
        return idToken != null && !idToken.isEmpty();
    }
}
