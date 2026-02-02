package com.project.backend.domain.notification.service.query;

import com.project.backend.domain.notification.dto.response.NotificationResDTO;

public interface NotificationQueryService {

    NotificationResDTO.BriefingRes getBriefing(Long memberId);
}
