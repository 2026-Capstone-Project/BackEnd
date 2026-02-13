package com.project.backend.domain.suggestion.scheduler;

import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import com.project.backend.domain.suggestion.service.command.SuggestionCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuggestionScheduler {

    private final MemberRepository memberRepository;
    private final SuggestionCommandService suggestionCommandService;
    private final SuggestionRepository suggestionRepository;

    @Value("${spring.scheduler.suggestion.enabled}")
    private boolean enabled;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void runDailySuggestion() {

        if (!enabled) {
            log.warn("선제적 제안 스케줄러가 비활성화 상태입니다");
        }

        List<Long> memberIds = memberRepository.findActiveMemberIdsWithSuggestionEnabled();
        log.info("[Suggestion Scheduler] : 선제적 제안 스케쥴러 시작 | memberCnt = {}", memberIds.size());

        for (Long memberId : memberIds) {
            try {
                log.info("[Suggestion Scheduler] : 개발 단계에 있으므로 모든 제안을 삭제 후 재생성 합니다");
                suggestionRepository.deleteAllByMemberId(memberId);
                suggestionCommandService.createEventSuggestion(memberId);
                suggestionCommandService.createTodoSuggestion(memberId);
                suggestionCommandService.createRecurrenceSuggestion(memberId);
                suggestionCommandService.createTodoRecurrenceSuggestion(memberId);
            } catch (Exception e) {
                log.warn("[Suggestion Scheduler] : 실패 | memberId = {} | failed = {}", memberId, e.getMessage());
            }
        }

        log.info("[Suggestion Scheduler] : 선제적 제안 스케쥴러 종료");
    }
}
