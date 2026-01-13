package com.project.backend.domain.auth.strategy;

import com.project.backend.domain.auth.converter.AuthConverter;
import com.project.backend.domain.auth.dto.OAuthTokenResult;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.exception.AuthErrorCode;
import com.project.backend.domain.auth.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KakaoOAuthStrategy implements OAuthStrategy {

    @Value("${spring.security.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.kakao.client-secret}")
    private String clientSecret;

    @Value("${spring.security.kakao.redirect-uri}")
    private String redirectUri;

    private static final String AUTHORIZATION_URI = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromUriString(AUTHORIZATION_URI)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public OAuthTokenResult exchangeCodeForToken(String code, String state) {
        AuthResDTO.KakaoTokenResponse tokenResponse;

        try {
            tokenResponse = WebClient.create(TOKEN_URI)
                    .post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(buildTokenRequestBody(code))
                    .retrieve()
                    .bodyToMono(AuthResDTO.KakaoTokenResponse.class)
                    .block();
        } catch (WebClientRequestException e) {
            throw new AuthException(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH_REQUEST);
        }

        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new AuthException(AuthErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        }

        return OAuthTokenResult.builder()
                .accessToken(tokenResponse.accessToken())
                .refreshToken(tokenResponse.refreshToken())
                .provider(Provider.KAKAO)
                .build();
    }

    @Override
    public AuthResDTO.UserAuth fetchUserAuth(OAuthTokenResult tokenResult) {
        AuthResDTO.KakaoUserInfo kakaoUserInfo;

        try {
            kakaoUserInfo = WebClient.create(USER_INFO_URI)
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResult.accessToken())
                    .retrieve()
                    .bodyToMono(AuthResDTO.KakaoUserInfo.class)
                    .block();
        } catch (WebClientRequestException e) {
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

    private String buildTokenRequestBody(String code) {
        StringBuilder body = new StringBuilder();
        body.append("grant_type=authorization_code");
        body.append("&client_id=").append(clientId);
        body.append("&redirect_uri=").append(redirectUri);
        body.append("&code=").append(code);
        if (clientSecret != null && !clientSecret.isEmpty()) {
            body.append("&client_secret=").append(clientSecret);
        }
        return body.toString();
    }
}
