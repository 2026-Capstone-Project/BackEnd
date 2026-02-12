package com.project.backend.domain.suggestion.service.command;

public interface SuggestionCommandService {
    void createSuggestion(Long memberId);

    void createTodoSuggestion(Long memberId);

    void createRecurrenceSuggestion(Long memberId);

    void createTodoRecurrenceSuggestion(Long memberId);

    void acceptSuggestion(Long memberId, Long suggestionId);

    void rejectSuggestion(Long memberId, Long suggestionId);
}
