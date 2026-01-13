package com.project.backend.domain.auth.naver;

import com.project.backend.domain.auth.converter.AuthConverter;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.service.command.AuthCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

@Service
@Transactional
@RequiredArgsConstructor
public class NaverService {

    @Value("${spring.security.naver.client.id}") String clientId;
    @Value("${spring.security.naver.client.secret}") String clientSecret;
    @Value("${spring.security.naver.authorization-uri}") String authorizationUri;
    @Value("${spring.security.naver.redirect-uri}") String redirectUri;
    @Value("${spring.security.naver.token-uri}") String tokenUri;
    @Value("${spring.security.naver.user-info-uri}") String userInfoUri;

    private final AuthCommandService authCommandService;

    // 로그인을 통해 네이버로부터 인가 코드를 발급
    public void redirectToNaver(HttpServletResponse response, HttpSession session) throws IOException {

        // 인가 코드 발급 전 state 세션 생성
        String state = generateState(session);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .toUriString();

        // code 발급을 위한 redirect
        response.sendRedirect(redirectUrl);
    }

    public void callback(String code, String state, HttpServletResponse response, HttpSession session) {

        // 세션 또는 별도의 저장 공간에서 상태 토큰을 가져옴
        String storedState = session.getAttribute("state").toString();

        // 만약 다르다면 악의적 요청으로 간주 -> 예외
        if (!state.equals(storedState)) {
            throw new IllegalStateException("Invalid state");
        }
        // Naver access token 발급
        String accessToken = getAccessTokenFromNaver(code, state);

        // 사용자 정보 return
        AuthResDTO.UserAuth userAuth = getNaverUserAuth(accessToken);

        // Login or Signup
        authCommandService.loginOrSignup(response, userAuth);
    }

    // 실제 사용자의 요청인지 검증할 state 난수 생성 후 세션에 저장
    private String generateState(HttpSession session) {

        SecureRandom random = new SecureRandom();
        String state = new BigInteger(130, random).toString(32);
        session.setAttribute("state", state);

        return state;
    }

    // Naver Access Token 발급 메서드
    private String getAccessTokenFromNaver(String code, String state) {

        AuthResDTO.NaverToken naverToken =
                WebClient.create(tokenUri)
                        .get()
                        .uri(uriBuilder -> uriBuilder
                                .queryParam("client_id", clientId)
                                .queryParam("client_secret", clientSecret)
                                .queryParam("grant_type", "authorization_code")
                                .queryParam("state", state)
                                .queryParam("code", code)
                                .build()
                        )
                        .retrieve()
                        .bodyToMono(AuthResDTO.NaverToken.class)
                        .block();

        if (naverToken == null) {
            throw new IllegalStateException("토큰이 존재하지 않음");
        }

        return naverToken.access_token();
    }

    // Naver Access Token을 사용해 유저 정보 조회 후 반환
    private AuthResDTO.UserAuth getNaverUserAuth(String accessToken) {

        AuthResDTO.NaverInfo naverInfo =
                WebClient.create(userInfoUri)
                        .get()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .bodyToMono(AuthResDTO.NaverInfo.class)
                        .block();

        if (naverInfo == null || naverInfo.response() == null) {
            throw new IllegalStateException("네이버 유저 정보가 존재하지 않음");
        }

        return AuthConverter.toUserAuth(naverInfo, Provider.NAVER);
    }
}
