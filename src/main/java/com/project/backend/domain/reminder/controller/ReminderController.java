package com.project.backend.domain.reminder.controller;

import com.project.backend.domain.reminder.dto.response.ReminderResDTO;
import com.project.backend.domain.reminder.service.query.ReminderQueryService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reminders")
public class ReminderController implements ReminderDocs {

    private final ReminderQueryService reminderQueryService;

    @GetMapping("")
    @Override
    public CustomResponse<List<ReminderResDTO.DetailRes>> getReminders(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        List<ReminderResDTO.DetailRes> res = reminderQueryService.getReminder(customUserDetails.getId());
        return CustomResponse.onSuccess("리마인더 조회 완료", res);
    }
}
