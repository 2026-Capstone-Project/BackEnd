package com.project.backend.domain.auth.converter;

import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.entity.Auth;
import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.google.GoogleIdTokenPayload;
import com.project.backend.domain.member.entity.Member;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthConverter {

    public static AuthResDTO.UserAuth toUserAuth(GoogleIdTokenPayload payload, Provider provider) {

        return AuthResDTO.UserAuth.builder()
                .provider(provider)
                .providerId(payload.getSub())
                .email(payload.getEmail())
                .name(payload.getName())
                .build();
    }

    public static Auth toAuth(AuthResDTO.UserAuth userAuth, Member member) {

        return Auth.builder()
                .provider(userAuth.provider())
                .providerId(userAuth.providerId())
                .member(member)
                .build();
    }
}
