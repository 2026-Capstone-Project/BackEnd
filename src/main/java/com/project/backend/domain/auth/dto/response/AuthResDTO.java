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
}
