package com.project.backend.global.security.jwt;

import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.member.enums.Role;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    @Getter
    private final long accessExpMs;
    @Getter
    private final long refreshExpMs;
    private final RedisTemplate<String, String> redisTemplate;

    public JwtUtil(
            @Value("${spring.jwt.secret}") String secret,
            @Value("${spring.jwt.token.access-expiration-time}") long accessExpMs,
            @Value("${spring.jwt.token.refresh-expiration-time}") long refreshExpMs,
            RedisTemplate<String, String> redisTemplate
    ) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        this.accessExpMs = accessExpMs;
        this.refreshExpMs = refreshExpMs;
        this.redisTemplate = redisTemplate;
    }

    // 토큰에서 id를 추출
    public Long getId(String token) throws SignatureException {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("id", Long.class);
    }


    // 토큰에서 Email을 추출
    public String getEmail(String token) throws SignatureException {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email", String.class);
    }

    public String getSubject(String token) throws SignatureException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // 토큰 남은 만료시간 (초)
    public long getRemainingTime(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remainingMs / 1000);
        } catch (ExpiredJwtException e) {
            return 0;
        }
    }

    // 토큰 파싱 (Claims 추출)
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void addToBlacklist(String token, long remainingTimeSeconds) {
        if (remainingTimeSeconds > 0) {
            redisTemplate.opsForValue().set(
                    getJti(token) + ":blacklist",
                    "logout",
                    remainingTimeSeconds,
                    TimeUnit.SECONDS
            );
        }
    }

    // 토큰에서 Role을 추출
    public Role getRoles(String token) throws SignatureException {

        String roleStr = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
        return Role.valueOf(roleStr);
    }

    // 토큰에서 jti를 추출
    public String getJti(String token) throws SignatureException {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("jti", String.class);
    }

    public Provider getProvider(String token) throws SignatureException {

        String providerStr = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("provider", String.class);
        return Provider.valueOf(providerStr);
    }

    // 토큰에서 providerId를 추출
    public String getProviderId(String token) throws SignatureException {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("providerId", String.class);
    }

    public String tokenIssuer(CustomUserDetails user, Instant exp) {

        // 현재 시간
        Instant issuedAt = Instant.now();
        // 토큰에 부여할 고유 jti (블랙 리스트에서 활용)
        final String jti = UUID.randomUUID().toString();

        // 토큰에 부여할 권한 (User, Admin 단일 권한이므로 첫 번째 요소가 role)
        String authorities = user.getAuthorities().stream()
                .findFirst()
                .orElseThrow()
                .getAuthority();

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("role", authorities);
        claims.put("email", user.getEmail());
        claims.put("providerId", user.getProviderId());
        claims.put("provider", user.getProvider());

        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .and()
                .id(jti)
                .subject(user.getUsername())
                .claims(claims)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    public String createJwtAccessToken(CustomUserDetails user) {

        Instant expiration = Instant.now().plusMillis(accessExpMs);
        return tokenIssuer(user, expiration);
    }

    public String createJwtRefreshToken(CustomUserDetails user) {

        Instant expiration = Instant.now().plusMillis(refreshExpMs);
        String refreshToken = tokenIssuer(user, expiration);

        // 리프레시 토큰 저장
        redisTemplate.opsForValue().set(
                user.getUsername() + ":refresh",
                refreshToken,
                refreshExpMs,
                TimeUnit.MILLISECONDS
        );

        return refreshToken;
    }

    // 제공된 리프레시 토큰을 기반으로 access token을 다시 발급
    public String reissueToken(String refreshToken) throws SignatureException {
        // refreshToken 에서 user 정보를 가져와서 새로운 토큰을 발급 (발급 시간, 유효 시간(reset)만 새로 적용)
        // 재발급시에는 비밀번호를 넣지 않아 비밀번호 노출 억제
        CustomUserDetails userDetails = new CustomUserDetails(
                getId(refreshToken),
                getProvider(refreshToken),
                getProviderId(refreshToken),
                getEmail(refreshToken),
                getRoles(refreshToken)
        );
        log.info("[ JwtUtil ] 새로운 토큰을 재발급");

        // 재발급
        return createJwtAccessToken(userDetails);
    }

    public void validateToken(String token) {
        log.debug("[ JwtUtil ] 토큰의 유효성 검증");
        try {
            // 구문 분석 시스템의 시계가 JWT를 생성한 시스템의 시계 오차 고려
            // 약 3분 허용.
            long seconds = 3 * 60;
            boolean isExpired = Jwts
                    .parser()
                    .clockSkewSeconds(seconds)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .before(new Date());
            if (isExpired) {
                log.debug("만료된 JWT 토큰");
            }

        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            //원하는 Exception throw
            throw new SecurityException("잘못된 토큰");
        } catch (ExpiredJwtException e) {
            //원하는 Exception throw
            throw new ExpiredJwtException(null, null, "만료된 JWT 토큰");
        }
    }
}
