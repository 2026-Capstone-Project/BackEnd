package com.project.backend.domain.member.scheduler;

import com.project.backend.domain.auth.repository.AuthRepository;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberCleanupScheduler {

    private static final int RETENTION_MONTHS = 3;

    private final MemberRepository memberRepository;
    private final AuthRepository authRepository;

    /**
     * 탈퇴 후 3개월이 지난 회원 Hard Delete
     * - 매일 새벽 3시에 실행
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupDeletedMembers() {
        log.info("탈퇴 회원 정리 스케줄러 시작");

        LocalDateTime threshold = LocalDateTime.now().minusMonths(RETENTION_MONTHS);
        List<Member> expiredMembers = memberRepository.findAllDeletedBefore(threshold);

        if (expiredMembers.isEmpty()) {
            log.info("정리 대상 탈퇴 회원이 없습니다.");
            return;
        }

        log.info("정리 대상 탈퇴 회원 수: {}", expiredMembers.size());

        for (Member member : expiredMembers) {
            try {
                // Auth 삭제 (재가입 방지 기간이 지났으므로 삭제)
                authRepository.deleteByMemberId(member.getId());

                // Member Hard Delete
                memberRepository.delete(member);

                log.info("회원 Hard Delete 완료: memberId={}, deletedAt={}", member.getId(), member.getDeletedAt());
            } catch (Exception e) {
                log.error("회원 Hard Delete 실패: memberId={}, error={}", member.getId(), e.getMessage());
            }
        }

        log.info("탈퇴 회원 정리 스케줄러 완료: 처리된 회원 수={}", expiredMembers.size());
    }
}
