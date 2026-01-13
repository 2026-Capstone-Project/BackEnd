package com.project.backend.global.security.service;

import com.project.backend.global.apiPayload.exception.CustomException;
import com.project.backend.global.security.exception.SecurityErrorCode;
import com.project.backend.global.security.jwt.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public String reissueCookie(String refreshToken) {
        // 4. 토큰 만료 시 refreshToken으로 AccessToken 재발급

        if (refreshToken == null) {
            // 리프레시 토큰 조차 만료 -> 재 로그인 안내
            throw new CustomException(SecurityErrorCode.REQUIRED_RE_LOGIN);
        }

        // refresh token의 유효성 검사
        log.debug("[ reissueCookie ] refresh token의 유효성을 검사합니다.");
        try {
            jwtUtil.validateToken(refreshToken);
            // redis 에 해당 refresh token이 존재하는지 검사
            if (!redisTemplate.hasKey(jwtUtil.getSubject(refreshToken)+ ":refresh")) {
                // 서버에 리프레시 토큰이 없음 -> 재 로그인 안내
                throw new CustomException(SecurityErrorCode.REQUIRED_RE_LOGIN);
            }
        } catch (ExpiredJwtException e) {
            throw new CustomException(SecurityErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        // access token 재발급
        log.debug("[ reissueCookie ] refresh token 으로 access token 을 생성합니다.");
        return jwtUtil.reissueToken(refreshToken);
    }
}
