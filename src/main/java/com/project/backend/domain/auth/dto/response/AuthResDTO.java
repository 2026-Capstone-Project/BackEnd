package com.project.backend.domain.auth.dto.response;

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
