package com.project.backend.domain.suggestion.service.batch;
import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import com.project.backend.domain.suggestion.service.command.SuggestionCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SuggestionBatchServiceImpl implements SuggestionBatchService{

    private final SuggestionCommandService suggestionCommandService;
    private final SuggestionRepository suggestionRepository;

    @Override
    public void regenerateAllForMember(Long memberId) {
        log.info("[Suggestion Scheduler] : 개발 단계에 있으므로 모든 제안을 삭제 후 재생성 합니다 | memberId={}", memberId);

        suggestionRepository.deleteAllByMemberId(memberId);

        suggestionCommandService.createEventSuggestion(memberId);
        suggestionCommandService.createTodoSuggestion(memberId);
        suggestionCommandService.createRecurrenceSuggestion(memberId);
        suggestionCommandService.createTodoRecurrenceSuggestion(memberId);
    }
}
