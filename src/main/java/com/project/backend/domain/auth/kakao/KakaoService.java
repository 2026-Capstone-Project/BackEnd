package com.project.backend.domain.auth.kakao;

import com.project.backend.domain.auth.converter.AuthConverter;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.exception.AuthErrorCode;
import com.project.backend.domain.auth.exception.AuthException;
import com.project.backend.domain.auth.service.command.AuthCommandService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
public class KakaoService {

    // 카카오 OAuth 설정
    @Value("${spring.security.kakao.client-id}") String clientId;
    @Value("${spring.security.kakao.client-secret}") String clientSecret;
    @Value("${spring.security.kakao.redirect-uri}") String redirectUri;

    // 카카오 OAuth URL (고정값)
    private static final String AUTHORIZATION_URI = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final AuthCommandService authCommandService;

    /**
     * 카카오 로그인 페이지로 리다이렉트
     */
    public void redirectToKakao(HttpServletResponse response, HttpSession session) throws IOException {

        // 인가 코드 발급 전 state 세션 생성 (CSRF 방지)
        String state = generateState(session);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(AUTHORIZATION_URI)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .toUriString();

        // 카카오 로그인 페이지로 redirect
        response.sendRedirect(redirectUrl);
    }

    /**
     * 카카오 OAuth Callback 처리
     */
    public void callback(String code, String state, HttpServletResponse response, HttpSession session) {

        // 세션에서 저장된 state 토큰 가져오기
        String storedState = (String) session.getAttribute("kakao_state");

        // state 검증 (CSRF 방지)
        if (storedState == null || !Objects.equals(storedState, state)) {
            throw new AuthException(AuthErrorCode.OAUTH_STATE_MISMATCH);
        }

        // 사용된 세션 삭제
        session.removeAttribute("kakao_state");

        // 카카오 access token 발급
        String accessToken = getAccessTokenFromKakao(code);

        // 사용자 정보 조회
        AuthResDTO.UserAuth userAuth = getKakaoUserAuth(accessToken);

        // 로그인 또는 회원가입 처리
        authCommandService.loginOrSignup(response, userAuth);
    }

    /**
     * CSRF 방지용 state 난수 생성 후 세션에 저장
     */
    private String generateState(HttpSession session) {
        SecureRandom random = new SecureRandom();
        String state = new BigInteger(130, random).toString(32);
        session.setAttribute("kakao_state", state);
        return state;
    }

    /**
     * 카카오 Access Token 발급
     */
    private String getAccessTokenFromKakao(String code) {
        AuthResDTO.KakaoTokenResponse tokenResponse;

        try {
            tokenResponse = WebClient.create(TOKEN_URI)
                    .post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(
                            "grant_type=authorization_code" +
                            "&client_id=" + clientId +
                            "&redirect_uri=" + redirectUri +
                            "&code=" + code +
                            (clientSecret != null && !clientSecret.isEmpty() ? "&client_secret=" + clientSecret : "")
                    )
                    .retrieve()
                    .bodyToMono(AuthResDTO.KakaoTokenResponse.class)
                    .block();
        } catch (WebClientRequestException e) {
            // 네트워크 / 타임아웃 오류
            throw new AuthException(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH_REQUEST);
        }

        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new AuthException(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        }

        return tokenResponse.accessToken();
    }

    /**
     * 카카오 Access Token을 사용해 사용자 정보 조회
     */
    private AuthResDTO.UserAuth getKakaoUserAuth(String accessToken) {
        AuthResDTO.KakaoUserInfo kakaoUserInfo;

        try {
            kakaoUserInfo = WebClient.create(USER_INFO_URI)
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(AuthResDTO.KakaoUserInfo.class)
                    .block();
        } catch (WebClientRequestException e) {
            // 네트워크 / 타임아웃 오류
            throw new AuthException(AuthErrorCode.KAKAO_USER_INFO_REQUEST_FAILED);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH_REQUEST);
        }

        if (kakaoUserInfo == null || kakaoUserInfo.id() == null) {
            throw new AuthException(AuthErrorCode.KAKAO_USER_INFO_REQUEST_FAILED);
        }

        return AuthConverter.toUserAuth(kakaoUserInfo, Provider.KAKAO);
    }
}
