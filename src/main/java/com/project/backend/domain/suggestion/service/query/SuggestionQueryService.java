package com.project.backend.domain.suggestion.service.query;


import com.project.backend.domain.suggestion.dto.response.SuggestionResDTO;

public interface SuggestionQueryService {

    SuggestionResDTO.SuggestionListRes getSuggestions(Long memberId);
}
