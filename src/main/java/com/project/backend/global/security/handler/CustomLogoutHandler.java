package com.project.backend.global.security.handler;

import com.project.backend.global.security.jwt.JwtUtil;
import com.project.backend.global.security.utils.CookieUtil;
import com.project.backend.global.security.utils.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        log.info("로그아웃 처리 시작");

        // 1. 쿠키에서 토큰 추출
        String accessToken = cookieUtil.extractFromCookie(request, "access_token");
        String refreshToken = cookieUtil.extractFromCookie(request, "refresh_token");

        // 2. Access Token 블랙리스트 등록
        if (accessToken != null) {
            try {
                long remainingTime = jwtUtil.getRemainingTime(accessToken);
                if (remainingTime > 0) {
                    jwtUtil.addToBlacklist(accessToken, remainingTime);
                    log.debug("Access Token 블랙리스트 등록 완료");
                }
            } catch (Exception e) {
                log.warn("Access Token 블랙리스트 등록 실패: {}", e.getMessage());
            }
        }

        // 3. Refresh Token 처리
        if (refreshToken != null) {
            try {
                String email = jwtUtil.getEmail(refreshToken);

                // Refresh Token 블랙리스트 등록
                long remainingTime = jwtUtil.getRemainingTime(refreshToken);
                if (remainingTime > 0) {
                    jwtUtil.addToBlacklist(refreshToken, remainingTime);
                }

                // Redis에서 Refresh Token 삭제
                redisTemplate.delete("refresh:" + email);
                log.debug("Refresh Token 삭제 완료");

            } catch (Exception e) {
                log.warn("Refresh Token 처리 실패: {}", e.getMessage());
            }
        }

        // 4. 쿠키 삭제
        cookieUtil.deleteCookie(response, "access_token");
        cookieUtil.deleteCookie(response, "refresh_token");

        log.info("로그아웃 처리 완료");

        try {
            HttpResponseUtil.setSuccessResponse(response, HttpStatus.OK, "로그아웃 완료");
        } catch (IOException e) {
            log.error("로그아웃 응답 작성 실패: {}", e.getMessage());
            response.setStatus(HttpStatus.OK.value());
        }
    }
}
