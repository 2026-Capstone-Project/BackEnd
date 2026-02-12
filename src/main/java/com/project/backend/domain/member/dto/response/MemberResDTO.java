package com.project.backend.domain.member.dto.response;

import com.project.backend.domain.auth.enums.Provider;
import lombok.Builder;

public class MemberResDTO {

    /**
     * 사용자 기본 정보 응답 DTO
     * - 설정 페이지 상단에 표시할 사용자 정보
     */
    @Builder
    public record MyInfo(
            Long memberId,
            String nickname,
            String email,
            Provider provider
    ) {
    }
}
