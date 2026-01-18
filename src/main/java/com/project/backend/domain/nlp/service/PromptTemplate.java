package com.project.backend.domain.nlp.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

@Slf4j
@Component
public class PromptTemplate {

    @Value("classpath:prompts/nlp-system-prompt.txt")
    private Resource systemPromptResource;

    private String systemPromptTemplate;

    @PostConstruct
    public void init() {
        try {
            systemPromptTemplate = new String(
                    systemPromptResource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            log.info("시스템 프롬프트 로드 완료");
        } catch (IOException e) {
            log.error("시스템 프롬프트 로드 실패", e);
            throw new IllegalStateException("시스템 프롬프트를 로드할 수 없습니다", e);
        }
    }

    public String getSystemPrompt(LocalDate baseDate) {
        String dayOfWeek = baseDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.KOREAN);

        return systemPromptTemplate
                .replace("{current_date}", baseDate.toString())
                .replace("{day_of_week}", dayOfWeek);
    }

    public String getUserPrompt(String userInput) {
        return String.format("""
                사용자 입력: "%s"

                위 입력을 분석하여 일정(Event)인지 할 일(Todo)인지 구분하고, JSON 형식으로 응답해주세요.
                """, userInput);
    }
}
