package com.project.backend.domain.auth.strategy;

import com.project.backend.domain.auth.converter.AuthConverter;
import com.project.backend.domain.auth.dto.OAuthTokenResult;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.exception.AuthErrorCode;
import com.project.backend.domain.auth.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NaverOAuthStrategy implements OAuthStrategy {

    @Value("${spring.security.naver.client.id}")
    private String clientId;

    @Value("${spring.security.naver.client.secret}")
    private String clientSecret;

    @Value("${spring.security.naver.authorization-uri}")
    private String authorizationUri;

    @Value("${spring.security.naver.redirect-uri}")
    private String redirectUri;

    @Value("${spring.security.naver.token-uri}")
    private String tokenUri;

    @Value("${spring.security.naver.user-info-uri}")
    private String userInfoUri;

    @Override
    public Provider getProvider() {
        return Provider.NAVER;
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        return UriComponentsBuilder
                .fromUriString(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public OAuthTokenResult exchangeCodeForToken(String code, String state) {
        AuthResDTO.NaverToken naverToken;

        try {
            // Naver는 GET 방식으로 토큰 발급 (state 파라미터 필요)
            naverToken = WebClient.create(tokenUri)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("client_id", clientId)
                            .queryParam("client_secret", clientSecret)
                            .queryParam("grant_type", "authorization_code")
                            .queryParam("state", state)
                            .queryParam("code", code)
                            .build())
                    .retrieve()
                    .bodyToMono(AuthResDTO.NaverToken.class)
                    .block();
        } catch (WebClientRequestException e) {
            throw new AuthException(AuthErrorCode.NAVER_SERVER_ERROR);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH_REQUEST);
        }

        if (naverToken == null || naverToken.access_token() == null) {
            throw new AuthException(AuthErrorCode.NAVER_TOKEN_NOT_FOUND);
        }

        return OAuthTokenResult.builder()
                .accessToken(naverToken.access_token())
                .refreshToken(naverToken.refresh_token())
                .provider(Provider.NAVER)
                .build();
    }

    @Override
    public AuthResDTO.UserAuth fetchUserAuth(OAuthTokenResult tokenResult) {
        AuthResDTO.NaverInfo naverInfo;

        try {
            naverInfo = WebClient.create(userInfoUri)
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResult.accessToken())
                    .retrieve()
                    .bodyToMono(AuthResDTO.NaverInfo.class)
                    .block();
        } catch (WebClientRequestException e) {
            throw new AuthException(AuthErrorCode.NAVER_SERVER_ERROR);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH_REQUEST);
        }

        if (naverInfo == null || naverInfo.response() == null) {
            throw new AuthException(AuthErrorCode.NAVER_USER_INFO_NOT_FOUND);
        }

        return AuthConverter.toUserAuth(naverInfo, Provider.NAVER);
    }
}
