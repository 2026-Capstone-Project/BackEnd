package com.project.backend.domain.suggestion.service.command;

public interface SuggestionCommandService {
    void createSuggestion(Long memberId);

    void createRecurrenceSuggestion(Long memberId);

    void acceptSuggestion(Long memberId, Long suggestionId);
}
