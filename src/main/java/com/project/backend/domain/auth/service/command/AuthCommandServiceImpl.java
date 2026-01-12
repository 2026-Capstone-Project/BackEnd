package com.project.backend.domain.auth.service.command;

import com.project.backend.domain.auth.converter.AuthConverter;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.entity.Auth;
import com.project.backend.domain.auth.repository.AuthRepository;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.enums.Role;
import com.project.backend.domain.member.service.MemberService;
import com.project.backend.global.security.jwt.JwtUtil;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandServiceImpl implements AuthCommandService {

    private final AuthRepository authRepository;
    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    public void loginOrSignup(AuthResDTO.UserAuth userAuth) {

        Auth auth = authRepository
                .findByProviderAndProviderId(userAuth.provider(), userAuth.providerId())
                .orElseGet(() -> signup(userAuth));

        CustomUserDetails userDetails = new CustomUserDetails(auth.getMember().getId(), userAuth.providerId(), Role.ROLE_USER);
        jwtUtil.createJwtAccessToken(userDetails);
        jwtUtil.createJwtRefreshToken(userDetails);

        //login()
    }

    //private void login();

    private Auth signup(AuthResDTO.UserAuth userAuth) {

        Member member = memberService.createMember(userAuth);
        Auth auth = AuthConverter.toAuth(userAuth, member);
        authRepository.save(auth);
        return auth;
    }
}
