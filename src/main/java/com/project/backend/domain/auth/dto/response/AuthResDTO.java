package com.project.backend.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.backend.domain.auth.enums.Provider;
import lombok.Builder;

public class AuthResDTO {

    @Builder
    public record UserAuth(
            Provider provider,
            String providerId,
            String email,
            String name
    ) {
    }

    @Builder
    public record GoogleTokenRes(
            @JsonProperty("access_token")
            String accessToken,
            @JsonProperty("expires_in")
            Integer expiresIn,
            @JsonProperty("refresh_token")
            String refreshToken,
            @JsonProperty("scope")
            String scope,
            @JsonProperty("token_type")
            String tokenType,
            @JsonProperty("id_token")
            String idToken
    ) {
    }

    public record NaverInfo (
            Response response
    ) {
        public record Response(
                String id,
                String email,
                String name
        ) {
        }
    }

    public record NaverToken (
            String access_token,
            String refresh_token
    ) {
    }

    public record KakaoTokenResponse(
            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("refresh_token")
            String refreshToken,

            @JsonProperty("token_type")
            String tokenType,

            @JsonProperty("expires_in")
            Integer expiresIn
    ) {}

    public record KakaoUserInfo(
            Long id,

            @JsonProperty("kakao_account")
            KakaoAccount kakaoAccount
    ) {
        public record KakaoAccount(
                String email,
                Profile profile
        ) {
            public record Profile(
                    String nickname
            ) {}
        }

        public String getEmail() {
            return kakaoAccount != null ? kakaoAccount.email() : null;
        }

        public String getNickname() {
            if (kakaoAccount != null && kakaoAccount.profile() != null) {
                return kakaoAccount.profile().nickname();
            }
            return null;
        }
    }
}
