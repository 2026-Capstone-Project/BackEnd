package com.project.backend.domain.member.converter;

import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.enums.Role;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberConverter {

    public static Member toMember(AuthResDTO.UserAuth userAuth) {

        return Member.builder()
                .role(Role.ROLE_USER)
                .nickname(userAuth.name())
                .email(userAuth.email())
                .build();
    }
}
