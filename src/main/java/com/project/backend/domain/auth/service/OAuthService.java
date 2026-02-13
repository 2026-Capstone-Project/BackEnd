package com.project.backend.domain.auth.service;

import com.project.backend.domain.auth.dto.OAuthTokenResult;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.auth.entity.Auth;
import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.auth.exception.AuthErrorCode;
import com.project.backend.domain.auth.exception.AuthException;
import com.project.backend.domain.auth.repository.AuthRepository;
import com.project.backend.domain.auth.service.command.AuthCommandService;
import com.project.backend.domain.auth.strategy.OAuthStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * OAuth 소셜 로그인 통합 서비스
 * Strategy Pattern을 사용하여 여러 OAuth Provider를 하나의 서비스에서 처리
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OAuthService {

    private final List<OAuthStrategy> strategies;
    private final AuthCommandService authCommandService;
    private final AuthRepository authRepository;

    private static final int REJOIN_RESTRICTION_MONTHS = 3;

    private Map<Provider, OAuthStrategy> strategyMap;

    /**
     * Strategy Map 초기화 (Provider -> Strategy 매핑)
     */
    @PostConstruct
    public void initStrategyMap() {
        strategyMap = new EnumMap<>(Provider.class);
        for (OAuthStrategy strategy : strategies) {
            strategyMap.put(strategy.getProvider(), strategy);
        }
    }

    /**
     * OAuth 인가 페이지로 리다이렉트
     */
    public void redirectToProvider(Provider provider, HttpServletResponse response, HttpSession session)
            throws IOException {

        OAuthStrategy strategy = getStrategy(provider);

        // CSRF 방지용 state 생성 및 세션 저장
        String state = generateState();
        session.setAttribute(getStateSessionKey(provider), state);

        // 인가 URL 생성 및 리다이렉트
        String authorizationUrl = strategy.buildAuthorizationUrl(state);
        response.sendRedirect(authorizationUrl);
    }

    /**
     * OAuth Callback 처리
     */
    public void handleCallback(Provider provider, String code, String state,
                               HttpServletRequest request, HttpServletResponse response, HttpSession session) {

        // State 검증 (CSRF 방지)
        validateState(provider, state, session);

        OAuthStrategy strategy = getStrategy(provider);

        // 토큰 발급
        OAuthTokenResult tokenResult = strategy.exchangeCodeForToken(code, state);

        // 사용자 정보 조회
        AuthResDTO.UserAuth userAuth = strategy.fetchUserAuth(tokenResult);

        // 재가입 제한 검증 (탈퇴 후 3개월 이내인지 확인)
        validateRejoinRestriction(userAuth);

        // 로그인 또는 회원가입 처리
        authCommandService.loginOrSignup(request, response, userAuth);
    }

    /**
     * Provider에 해당하는 Strategy 조회
     */
    private OAuthStrategy getStrategy(Provider provider) {
        OAuthStrategy strategy = strategyMap.get(provider);
        if (strategy == null) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        return strategy;
    }

    /**
     * CSRF 방지용 state 난수 생성
     */
    private String generateState() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    /**
     * Provider별 세션 키 생성 (통일된 패턴)
     */
    private String getStateSessionKey(Provider provider) {
        return "OAUTH_STATE_" + provider.name();
    }

    /**
     * State 검증 및 세션 정리
     */
    private void validateState(Provider provider, String state, HttpSession session) {
        String sessionKey = getStateSessionKey(provider);
        String storedState = (String) session.getAttribute(sessionKey);

        if (storedState == null || !Objects.equals(storedState, state)) {
            throw new AuthException(AuthErrorCode.OAUTH_STATE_MISMATCH);
        }

        // 사용된 state 삭제 (재사용 방지)
        session.removeAttribute(sessionKey);
    }

    /**
     * 재가입 제한 검증
     * - 탈퇴한 회원이 3개월 이내에 재가입 시도하면 예외 발생
     * - 3개월 초과 시 정상 진행 (회원 복구는 loginOrSignup에서 처리)
     */
    private void validateRejoinRestriction(AuthResDTO.UserAuth userAuth) {
        Optional<Auth> deletedAuth = authRepository.findDeletedByProviderAndProviderId(
                userAuth.provider(),
                userAuth.providerId()
        );

        if (deletedAuth.isPresent()) {
            LocalDateTime deletedAt = deletedAuth.get().getMember().getDeletedAt();
            LocalDateTime rejoinAllowedAt = deletedAt.plusMonths(REJOIN_RESTRICTION_MONTHS);

            if (LocalDateTime.now().isBefore(rejoinAllowedAt)) {
                throw new AuthException(AuthErrorCode.REJOIN_RESTRICTED);
            }
            // 3개월 초과 시 정상 진행 - 회원 복구 로직은 authCommandService에서 처리
        }
    }
}
