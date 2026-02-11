package com.project.backend.domain.suggestion.controller;

import com.project.backend.domain.suggestion.dto.response.SuggestionResDTO;
import com.project.backend.domain.suggestion.service.command.SuggestionCommandService;
import com.project.backend.domain.suggestion.service.query.SuggestionQueryService;
import com.project.backend.domain.suggestion.vo.SuggestionCandidate;
import com.project.backend.domain.suggestion.vo.SuggestionKey;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionCommandService suggestionCommandService;
    private final SuggestionQueryService suggestionQueryService;

    @PostMapping()
    public CustomResponse<Map<SuggestionKey, List<SuggestionCandidate>>> createSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        suggestionCommandService.createSuggestion(customUserDetails.getId());
        return CustomResponse.onSuccess("선제적 제안 생성", null);
    }

    @PostMapping("/recurrences")
    public CustomResponse<?> createRecurrenceSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        suggestionCommandService.createRecurrenceSuggestion(customUserDetails.getId());
        return CustomResponse.onSuccess("반복 그룹에 대한 선제적 제안 생성 완료", null);
    }

    @GetMapping()
    public CustomResponse<SuggestionResDTO.SuggestionListRes> getSuggestions(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        SuggestionResDTO.SuggestionListRes resDTO =
                suggestionQueryService.getSuggestions(customUserDetails.getId());
        return CustomResponse.onSuccess("선제적 제안 목록 조회 성공", resDTO);
    }

}
