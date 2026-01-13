package com.project.backend.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleIdTokenPayload {

    // 검증용
    private String iss;
    private String aud;
    private Long exp;

    // 식별용 (핵심)
    private String sub;

    // 서비스용
    private String email;
    private String name;
}
