package com.project.backend.domain.suggestion.service.query;

import com.project.backend.domain.suggestion.converter.SuggestionConverter;
import com.project.backend.domain.suggestion.dto.response.SuggestionResDTO;
import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SuggestionQueryServiceImpl implements SuggestionQueryService {

    private final SuggestionRepository suggestionRepository;

    @Override
    public SuggestionResDTO.SuggestionListRes getSuggestions(Long memberId) {
        List<Suggestion> suggestions = suggestionRepository.findByMemberIdAndActiveIsTrueOrderByIdDesc(memberId);

        List<SuggestionResDTO.SuggestionDetailRes> suggestionListRes = suggestions.stream()
                .map(SuggestionConverter::toSuggestionDetailRes)
                .filter(Objects::nonNull)
                .toList();

        return SuggestionConverter.toSuggestionListRes(suggestionListRes);
    }
}
