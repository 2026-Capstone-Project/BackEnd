package com.project.backend.domain.suggestion.service.command;

import com.project.backend.domain.suggestion.vo.SuggestionCandidate;
import com.project.backend.domain.suggestion.vo.SuggestionKey;

import java.util.List;
import java.util.Map;

public interface SuggestionCommandService {
    Map<SuggestionKey, List<SuggestionCandidate>> createSuggestion(Long memberId);
}
