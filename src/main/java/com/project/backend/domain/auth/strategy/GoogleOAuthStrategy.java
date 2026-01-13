package com.project.backend.domain.auth.strategy;

import com.project.backend.domain.auth.converter.AuthConverter;
import com.project.backend.domain.auth.dto.OAuthTokenResult;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.exception.AuthErrorCode;
import com.project.backend.domain.auth.exception.AuthException;
import com.project.backend.domain.auth.dto.GoogleIdTokenPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class GoogleOAuthStrategy implements OAuthStrategy {

    private final ObjectMapper objectMapper;

    @Value("${spring.security.google.client-id}")
    private String clientId;

    @Value("${spring.security.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.google.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.google.token-uri}")
    private String tokenUri;

    private static final String AUTHORIZATION_URI = "https://accounts.google.com/o/oauth2/v2/auth";

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromUriString(AUTHORIZATION_URI)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public OAuthTokenResult exchangeCodeForToken(String code, String state) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        AuthResDTO.GoogleTokenRes tokenResponse = WebClient.create(tokenUri)
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(AuthResDTO.GoogleTokenRes.class)
                .block();

        if (tokenResponse == null || tokenResponse.idToken() == null) {
            throw new AuthException(AuthErrorCode.GOOGLE_TOKEN_REQUEST_FAILED);
        }

        return OAuthTokenResult.builder()
                .accessToken(tokenResponse.accessToken())
                .idToken(tokenResponse.idToken())
                .provider(Provider.GOOGLE)
                .build();
    }

    @Override
    public AuthResDTO.UserAuth fetchUserAuth(OAuthTokenResult tokenResult) {
        // Google은 ID Token에서 사용자 정보 추출 (별도 API 호출 불필요)
        GoogleIdTokenPayload payload = decodeIdToken(tokenResult.idToken());

        // ID Token 검증 (Google 특이사항)
        validateIdTokenPayload(payload);

        return AuthConverter.toUserAuth(payload, Provider.GOOGLE);
    }

    private GoogleIdTokenPayload decodeIdToken(String idToken) {
        try {
            String payloadBase64 = idToken.split("\\.")[1];
            String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64));
            return objectMapper.readValue(payloadJson, GoogleIdTokenPayload.class);
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.GOOGLE_ID_TOKEN_DECODE_FAILED);
        }
    }

    private void validateIdTokenPayload(GoogleIdTokenPayload payload) {
        // iss 검증: Google 발급 여부
        if (!"https://accounts.google.com".equals(payload.getIss())
                && !"accounts.google.com".equals(payload.getIss())) {
            throw new AuthException(AuthErrorCode.GOOGLE_ISS_VALIDATION_FAILED);
        }

        // aud 검증: 우리 앱을 위한 토큰인지
        if (!clientId.equals(payload.getAud())) {
            throw new AuthException(AuthErrorCode.GOOGLE_AUD_VALIDATION_FAILED);
        }

        // exp 검증: 토큰 만료 여부
        if (payload.getExp() * 1000 < System.currentTimeMillis()) {
            throw new AuthException(AuthErrorCode.GOOGLE_TOKEN_EXPIRED);
        }
    }
}
