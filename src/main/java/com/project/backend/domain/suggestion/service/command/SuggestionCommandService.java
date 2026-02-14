package com.project.backend.domain.suggestion.service.command;

public interface SuggestionCommandService {
    void createEventSuggestion(Long memberId);

    void createTodoSuggestion(Long memberId);

    void createRecurrenceSuggestion(Long memberId);

    void createTodoRecurrenceSuggestion(Long memberId);

    void acceptSuggestion(Long memberId, Long suggestionId);

    void rejectSuggestion(Long memberId, Long suggestionId);

    // 프론트 전용 메서드
    void delete(Long memberId);

    void createSuggestion(Long memberId);
}
