package com.project.backend.domain.briefing.controller;

import com.project.backend.domain.briefing.dto.response.BriefingResDTO;
import com.project.backend.domain.briefing.service.query.BriefingQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/briefings")
public class BriefingController implements BriefingDocs{

    private final BriefingQueryService briefingQueryService;

    @Override
    @GetMapping("")
    public CustomResponse<BriefingResDTO.BriefingRes> getBriefing(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        BriefingResDTO.BriefingRes res = briefingQueryService.getBriefing(customUserDetails.getId());
        return CustomResponse.onSuccess("오늘의 브리핑 조회 완료", res);
    }

}
