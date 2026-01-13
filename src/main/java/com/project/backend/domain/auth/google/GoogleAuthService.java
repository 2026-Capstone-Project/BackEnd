package com.project.backend.domain.auth.google;


import com.project.backend.domain.auth.converter.AuthConverter;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.entity.Auth;
import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.repository.AuthRepository;
import com.project.backend.domain.auth.service.command.AuthCommandService;
import com.project.backend.domain.auth.service.command.AuthCommandServiceImpl;
import com.project.backend.domain.member.enums.Role;
import com.project.backend.domain.member.service.MemberService;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.global.security.jwt.JwtUtil;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GoogleAuthService {

    private final ObjectMapper objectMapper;
    private final AuthCommandService authCommandService;
    private final AuthRepository authRepository;
    private final MemberService memberService;
    private final JwtUtil jwtUtil;

    @Value("${spring.security.google.client-id}")
    private String clientId;

    @Value("${spring.security.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.google.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.google.token-uri}")
    private String tokenUri;

    public String generateAuthorizationUrl(HttpSession session) {

        // Csrf 방어용 state 값 생성
        String state = UUID.randomUUID().toString();

        // callback 단계에서 검증하기 위해 세션에 저장
        session.setAttribute("GOOGLE_OAUTH_STATE", state);

        // google oauth2 인증 요청 url 생성
        return UriComponentsBuilder
                .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    public void exchangeCode(String code, String state, HttpServletResponse response, HttpSession session) {

        // state 검증
        String savedState = (String) session.getAttribute("GOOGLE_OAUTH_STATE");
        if (!state.equals(savedState)) {
            throw new IllegalStateException("Invalid state");
        }
        // state 검증 후 세션에서 제거 (재사용 가능, csrf 공격 가능성)
        session.removeAttribute("GOOGLE_OAUTH_STATE");

        // Body (공식문서 그대로)
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        // webClient POST
        AuthResDTO.GoogleTokenRes token =
                WebClient.create(tokenUri)
                        .post()
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(AuthResDTO.GoogleTokenRes.class)
                        .block();

        if (token == null) {
            throw new IllegalStateException("Google 토큰 응답이 null");
        }

        // id_토큰에서 사용자 정보 추출
        AuthResDTO.UserAuth userAuth = extractUserInfo(token);

        // 로그인 여부 확인
        authCommandService.loginOrSignup(response, userAuth);
    }

    /**
     * Step 3: id_token 디코딩 → 사용자 식별
     */
    private AuthResDTO.UserAuth extractUserInfo(AuthResDTO.GoogleTokenRes tokenResponse) {

        String idToken = tokenResponse.idToken();
        if (idToken == null) {
            throw new IllegalStateException("id_token 이 존재하지 않습니다.");
        }

        // jwt 디코딩
        GoogleIdTokenPayload payload = decodeIdToken(idToken);

        // 최소한의 보안 검증
        validatePayload(payload);

        return AuthConverter.toUserAuth(payload, Provider.GOOGLE);
    }


    /**
     * JWT 디코딩 - 사용자 식별 정보만 꺼내기 위해
     */
    private GoogleIdTokenPayload decodeIdToken(String idToken) {

        try {
            String payloadBase64 = idToken.split("\\.")[1];
            String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64));

            return objectMapper.readValue(payloadJson, GoogleIdTokenPayload.class);

        } catch (Exception e) {
            throw new IllegalStateException("id_token 디코딩 실패", e);
        }
    }

    /**
     * 최소 검증
     * 이 로그인 증명서가 Google이 발급했고,
     * 우리 앱을 위한 것이며,
     * 아직 유효하다는 걸 보장하기 위한 최저선
     */
    private void validatePayload(GoogleIdTokenPayload payload) {

        if (!"https://accounts.google.com".equals(payload.getIss())
                && !"accounts.google.com".equals(payload.getIss())) {
            throw new IllegalStateException("iss 검증 실패");
        }

        if (!clientId.equals(payload.getAud())) {
            throw new IllegalStateException("aud(client_id) 검증 실패");
        }

        if (payload.getExp() * 1000 < System.currentTimeMillis()) {
            throw new IllegalStateException("id_token 만료");
        }
    }
}


