package com.project.backend.domain.auth.strategy;

import com.project.backend.domain.auth.dto.OAuthTokenResult;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.enums.Provider;

/**
 * OAuth 소셜 로그인 Strategy 인터페이스
 * 각 Provider별 구현체가 이 인터페이스를 구현합니다.
 */
public interface OAuthStrategy {

    /**
     * 이 Strategy가 처리하는 Provider 반환
     */
    Provider getProvider();

    /**
     * OAuth 인가 URL 생성
     * @param state CSRF 방지용 state 값
     * @return 리다이렉트할 OAuth 인가 URL
     */
    String buildAuthorizationUrl(String state);

    /**
     * 인가 코드로 토큰 발급
     * @param code OAuth 인가 코드
     * @param state CSRF 검증용 state 값 (Naver는 토큰 요청에 필요)
     * @return 토큰 결과 (accessToken 또는 idToken)
     */
    OAuthTokenResult exchangeCodeForToken(String code, String state);

    /**
     * 토큰 결과로 사용자 정보 조회/추출
     * @param tokenResult 토큰 발급 결과
     * @return 통합 사용자 정보 DTO
     */
    AuthResDTO.UserAuth fetchUserAuth(OAuthTokenResult tokenResult);
}
