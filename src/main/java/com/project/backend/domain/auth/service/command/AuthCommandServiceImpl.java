package com.project.backend.domain.auth.service.command;

import com.project.backend.domain.auth.converter.AuthConverter;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.entity.Auth;
import com.project.backend.domain.auth.repository.AuthRepository;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.enums.Role;
import com.project.backend.domain.member.service.MemberService;
import com.project.backend.global.security.csrf.repository.CustomCookieCsrfTokenRepository;
import com.project.backend.global.security.jwt.JwtUtil;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import com.project.backend.global.security.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandServiceImpl implements AuthCommandService {

    private final AuthRepository authRepository;
    private final MemberService memberService;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CustomCookieCsrfTokenRepository customCookieCsrfTokenRepository;

    @Value("${spring.jwt.token.access-expiration-time}")
    private long accessExpMs;
    @Value("${spring.jwt.token.refresh-expiration-time}")
    private long refreshExpMs;


    public void loginOrSignup(HttpServletRequest request, HttpServletResponse response, AuthResDTO.UserAuth userAuth) {

        Auth auth = authRepository
                .findByProviderAndProviderId(userAuth.provider(), userAuth.providerId())
                .orElseGet(() -> signup(userAuth));

        CustomUserDetails userDetails = new CustomUserDetails(auth.getMember().getId(), userAuth.provider(), userAuth.providerId(), userAuth.email(), Role.ROLE_USER);
        String accessToken = jwtUtil.createJwtAccessToken(userDetails);
        String refreshToken = jwtUtil.createJwtRefreshToken(userDetails);

        // 쿠키 생성하기
        cookieUtil.createJwtCookie(response, "access_token", accessToken, accessExpMs);
        cookieUtil.createJwtCookie(response, "refresh_token", refreshToken, refreshExpMs);

        // 토큰을 레디스에 등록
        redisTemplate.opsForValue().set(jwtUtil.getSubject(refreshToken) + ":refresh", refreshToken, accessExpMs);

        // 로그인 시 새로운 csrf 토큰 발급
        CsrfToken csrfToken = customCookieCsrfTokenRepository.generateToken(request);
        customCookieCsrfTokenRepository.saveToken(csrfToken, request, response);
    }

    private Auth signup(AuthResDTO.UserAuth userAuth) {

        Member member = memberService.createMember(userAuth);
        Auth auth = AuthConverter.toAuth(userAuth, member);
        authRepository.save(auth);
        return auth;
    }
}
