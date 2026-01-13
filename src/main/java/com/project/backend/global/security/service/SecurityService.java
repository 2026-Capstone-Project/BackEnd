package com.project.backend.global.security.service;

public interface SecurityService {

    String reissueCookie(String refreshToken);
}
