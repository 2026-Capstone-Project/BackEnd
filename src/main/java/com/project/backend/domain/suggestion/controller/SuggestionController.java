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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionCommandService suggestionCommandService;
    private final SuggestionQueryService suggestionQueryService;

    @GetMapping()
    public CustomResponse<SuggestionResDTO.SuggestionListRes> getSuggestions(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        SuggestionResDTO.SuggestionListRes resDTO =
                suggestionQueryService.getSuggestions(customUserDetails.getId());
        return CustomResponse.onSuccess("선제적 제안 목록 조회 성공", resDTO);
    }

    @PostMapping("/events")
    public CustomResponse<Map<SuggestionKey, List<SuggestionCandidate>>> createSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        suggestionCommandService.createEventSuggestion(customUserDetails.getId());
        return CustomResponse.onSuccess("선제적 제안 생성", null);
    }

    @PostMapping("/todos")
    public CustomResponse<String> createTodoSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        suggestionCommandService.createTodoSuggestion(customUserDetails.getId());
        return CustomResponse.onSuccess("투두 선제적 제안 생성 완료", null);
    }

    @PostMapping("/events/recurrences")
    public CustomResponse<String> createRecurrenceSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        suggestionCommandService.createRecurrenceSuggestion(customUserDetails.getId());
        return CustomResponse.onSuccess("반복 그룹에 대한 선제적 제안 생성 완료", null);
    }

    @PostMapping("/todos/recurrences")
    public CustomResponse<String> createRecurrenceTodoSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        suggestionCommandService.createTodoRecurrenceSuggestion(customUserDetails.getId());
        return CustomResponse.onSuccess("투두 반복 그룹에 대한 선제적 제안 생성 완료", null);
    }

    @PostMapping("/{suggestionId}/acceptance")
    public CustomResponse<String> acceptSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable("suggestionId") Long suggestionId
    ) {
        suggestionCommandService.acceptSuggestion(customUserDetails.getId(), suggestionId);
        return CustomResponse.onSuccess("선제적 제안 수락", null);
    }

    @PostMapping("{suggestionId}/rejection")
    public CustomResponse<String> rejectSuggestion(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PathVariable("suggestionId") Long suggestionId
    ) {
        suggestionCommandService.rejectSuggestion(customUserDetails.getId(), suggestionId);
        return CustomResponse.onSuccess("선제적 제안 거절", null);
    }

}
