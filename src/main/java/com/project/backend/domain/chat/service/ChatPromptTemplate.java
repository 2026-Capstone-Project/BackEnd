package com.project.backend.domain.chat.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ChatPromptTemplate {

    @Value("classpath:prompts/chat-system-prompt.txt")
    private Resource systemPromptResource;

    private String systemPrompt;

    @PostConstruct
    public void init() {
        try {
            systemPrompt = new String(
                    systemPromptResource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            log.debug("챗봇 시스템 프롬프트 로드 완료");
        } catch (IOException e) {
            log.error("챗봇 시스템 프롬프트 로드 실패", e);
            throw new IllegalStateException("챗봇 시스템 프롬프트를 로드할 수 없습니다", e);
        }
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }
}
