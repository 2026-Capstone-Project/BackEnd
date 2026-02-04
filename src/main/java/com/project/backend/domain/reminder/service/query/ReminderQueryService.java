package com.project.backend.domain.reminder.service.query;

import com.project.backend.domain.reminder.dto.response.ReminderResDTO;

import java.util.List;

public interface ReminderQueryService {

    List<ReminderResDTO.DetailRes> getReminder(Long memberId);
}
