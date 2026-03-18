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
        return systemPrompt.replace("{schedule_context}", "");
    }

    public String getSystemPrompt(String scheduleContext) {
        String contextBlock = scheduleContext != null
                ? "[사용자의 일정 및 할 일]\n" + scheduleContext + "\n\n위 내용을 참고하여 답변하세요. 일정과 할 일이 없을 경우 '해당 기간에 일정이 없어요'라고 답변하세요."
                : "";
        return systemPrompt.replace("{schedule_context}", contextBlock);
    }
}
