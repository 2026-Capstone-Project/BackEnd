package com.project.backend.domain.briefing.service.query;

import com.project.backend.domain.briefing.dto.response.BriefingResDTO;

public interface BriefingQueryService {

    BriefingResDTO.BriefingRes getBriefing(Long memberId);
}
