package com.project.backend.domain.suggestion.service.batch;

public interface SuggestionBatchService {

    void regenerateAllForMember(Long memberId);
}
