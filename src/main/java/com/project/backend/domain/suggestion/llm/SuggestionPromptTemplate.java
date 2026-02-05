package com.project.backend.domain.suggestion.llm;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// TODO : nlp 패키지에 있는 내용을 일단 개발을 위해서 중복 작성
@Slf4j
@Component
public class SuggestionPromptTemplate {

    @Value("classpath:prompts/suggestion-prompt.txt")
    private Resource systemPromptResource;

    private String systemPromptTemplate;

    @PostConstruct
    public void init() {
        try {
            systemPromptTemplate = new String(
                    systemPromptResource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            log.debug("시스템 프롬프트 로드 완료");
        } catch (IOException e) {
            log.error("시스템 프롬프트 로드 실패", e);
            throw new IllegalStateException("시스템 프롬프트를 로드할 수 없습니다", e);
        }
    }

    public String getSuggestionPrompt() {
        return systemPromptTemplate;
    }

    public String getUserSuggestionPrompt(String userInput) {
        return String.format("""
                "%s"

                위 입력을 분석하여 JSON 형식으로 응답해주세요.
                """, userInput);
    }

}
