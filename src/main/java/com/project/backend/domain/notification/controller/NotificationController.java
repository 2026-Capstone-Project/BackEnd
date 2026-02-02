package com.project.backend.domain.notification.controller;

import com.project.backend.domain.notification.dto.response.NotificationResDTO;
import com.project.backend.domain.notification.service.query.NotificationQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @GetMapping("/briefing")
    public CustomResponse<NotificationResDTO.BriefingRes> getBriefing(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        NotificationResDTO.BriefingRes res = notificationQueryService.getBriefing(customUserDetails.getId());
        return CustomResponse.onSuccess("오늘의 브리핑 조회 완료", res);
    }

}
