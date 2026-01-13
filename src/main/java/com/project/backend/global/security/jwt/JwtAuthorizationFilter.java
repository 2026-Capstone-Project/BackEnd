package com.project.backend.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import com.project.backend.global.security.utils.CookieUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final CookieUtil cookieUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Cookie에서 Access Token 추출
            String accessToken = cookieUtil.getTokenFromCookie(request, "access-token");

            // Access Token이 없다면 다음 필터
            if (accessToken == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 로그아웃 블랙리스트 확인
            if (redisTemplate.hasKey(jwtUtil.getJti(accessToken) + ":blacklist")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Access Token을 이용한 인증 처리
            authenticate(accessToken);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            CustomResponse<Object> errorResponse = CustomResponse.onFailure(
                    String.valueOf(HttpStatus.UNAUTHORIZED.value()),
                    "Access Token이 만료되었습니다.",
                    null
            );
            new ObjectMapper().writeValue(response.getWriter(), errorResponse);
        }
    }

    private void authenticate(String accessToken) {
        // Access Token의 유효성 검증
        jwtUtil.validateToken(accessToken);

        // Access Token으로 CustomUserDetails 생성
        CustomUserDetails user = new CustomUserDetails(jwtUtil.getId(accessToken), jwtUtil.getProviderId(accessToken), jwtUtil.getRoles(accessToken));

        // 인증 객체 생성
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        // 인증 객체를 SecurityContextHolder에 저장
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
