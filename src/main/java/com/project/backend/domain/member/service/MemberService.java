package com.project.backend.domain.member.service;

import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.repository.RecurrenceExceptionRepository;
import com.project.backend.domain.event.repository.RecurrenceGroupRepository;
import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.member.converter.MemberConverter;
import com.project.backend.domain.member.dto.response.MemberResDTO;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.setting.repository.SettingRepository;
import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import com.project.backend.domain.todo.repository.TodoRecurrenceExceptionRepository;
import com.project.backend.domain.todo.repository.TodoRecurrenceGroupRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import com.project.backend.global.security.jwt.JwtUtil;
import com.project.backend.global.security.utils.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.project.backend.domain.setting.converter.SettingConverter;
import com.project.backend.domain.setting.entity.Setting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final EventRepository eventRepository;
    private final TodoRecurrenceExceptionRepository todoRecurrenceExceptionRepository;
    private final TodoRecurrenceGroupRepository todoRecurrenceGroupRepository;
    private final TodoRepository todoRepository;
    private final SuggestionRepository suggestionRepository;
    private final SettingRepository settingRepository;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    public Member createMember(AuthResDTO.UserAuth userAuth){

        Member member = MemberConverter.toMember(userAuth);
        Setting setting = SettingConverter.toSetting(member);
        member.updateSetting(setting);
        return memberRepository.save(member);
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public MemberResDTO.MyInfo getMyInfo(Long memberId) {
        Member member = memberRepository.findActiveByIdWithAuth(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        return MemberConverter.toMyInfo(member);
    }

    // 회원 탈퇴
    @Transactional
    public void withdraw(Long memberId, HttpServletRequest request, HttpServletResponse response) {
        // 1. 회원 조회
        Member member = memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 2. 이미 탈퇴한 회원인지 확인
        if (member.isDeleted()) {
            throw new MemberException(MemberErrorCode.MEMBER_ALREADY_DELETED);
        }

        // 3. 연관 데이터 Hard Delete
        // Event ↔ RecurrenceGroup 양방향 FK 순환 참조를 끊기 위해 먼저 NULL 처리
        eventRepository.clearRecurrenceGroupByMemberId(memberId);
        recurrenceExceptionRepository.deleteAllByMemberId(memberId);
        recurrenceGroupRepository.deleteAllByMemberId(memberId);
        eventRepository.deleteAllByMemberId(memberId);
        // Todo ↔ TodoRecurrenceGroup 양방향 FK 순환 참조를 끊기 위해 먼저 NULL 처리
        todoRepository.clearTodoRecurrenceGroupByMemberId(memberId);
        todoRecurrenceExceptionRepository.deleteAllByMemberId(memberId);
        todoRecurrenceGroupRepository.deleteAllByMemberId(memberId);
        todoRepository.deleteAllByMemberId(memberId);
        suggestionRepository.deleteAllByMemberId(memberId);
        settingRepository.deleteByMemberId(memberId);

        // 4. Member Soft Delete
        member.delete();

        // 5. 토큰 무효화 처리
        invalidateTokens(request, response);
        log.info("회원 탈퇴 완료: memberId={}", memberId);
    }

    // 토큰 무효화 (Access Token, Refresh Token 블랙리스트 등록, Refresh Token 삭제)
    private void invalidateTokens(HttpServletRequest request, HttpServletResponse response) {
        // Access Token 블랙리스트 등록
        String accessToken = cookieUtil.getTokenFromCookie(request, "access_token");
        if (accessToken != null) {
            try {
                long remainingTime = jwtUtil.getRemainingTime(accessToken);
                if (remainingTime > 0) {
                    jwtUtil.addToBlacklist(accessToken, remainingTime);
                    log.debug("Access Token 블랙리스트 등록 완료");
                }
            } catch (Exception e) {
                log.warn("Access Token 블랙리스트 등록 실패: {}", e.getMessage());
            }
        }

        // Refresh Token 삭제
        String refreshToken = cookieUtil.getTokenFromCookie(request, "refresh_token");
        if (refreshToken != null) {
            try {
                String email = jwtUtil.getEmail(refreshToken);

                // Refresh Token 블랙리스트 등록
                long remainingTime = jwtUtil.getRemainingTime(refreshToken);
                if (remainingTime > 0) {
                    jwtUtil.addToBlacklist(refreshToken, remainingTime);
                }

                // Redis에서 Refresh Token 삭제
                redisTemplate.delete("refresh:" + email);
                log.debug("Refresh Token 삭제 완료");

            } catch (Exception e) {
                log.warn("Refresh Token 처리 실패: {}", e.getMessage());
            }
        }

        // 쿠키 삭제
        cookieUtil.deleteCookie(response, "access_token");
        cookieUtil.deleteCookie(response, "refresh_token");
    }
}
