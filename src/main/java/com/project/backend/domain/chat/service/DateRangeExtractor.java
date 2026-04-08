package com.project.backend.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.domain.chat.dto.DateRange;
import com.project.backend.domain.nlp.client.LlmClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DateRangeExtractor {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    @Value("classpath:prompts/date-range-extract-prompt.txt")
    private Resource promptResource;

    private String promptTemplate;

    // 파일을 매 요청마다 읽지 않고 서버 시작 시 한 번만 읽어 메모리에 올려두는 용도임
    @PostConstruct
    public void init() {
        try {
            promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
            log.debug("날짜 파싱 프롬프트 로드 완료");
        } catch (IOException e) {
            log.error("날짜 파싱 프롬프트 로드 실패", e);
            throw new IllegalStateException("날짜 파싱 프롬프트를 로드할 수 없습니다", e);
        }
    }

    public Optional<DateRange> extract(String message) {
        try {
            String systemPrompt = buildSystemPrompt(LocalDate.now());
            String raw = llmClient.chat(systemPrompt, message);
            return parseResponse(raw);
        } catch (Exception e) {
            log.warn("날짜 범위 추출 실패, 컨텍스트 없이 진행: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static final String[] DAY_LABELS = {"월", "화", "수", "목", "금", "토", "일"};

    private String buildSystemPrompt(LocalDate baseDate) {
        LocalDate thisMonday = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextMonday = thisMonday.plusWeeks(1);

        return promptTemplate
                .replace("{current_date}", baseDate.toString())
                .replace("{day_of_week}", baseDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN))
                .replace("{this_week}", formatWeekDays(thisMonday, baseDate))
                .replace("{next_week}", formatWeekDays(nextMonday, baseDate));
    }

    private String formatWeekDays(LocalDate monday, LocalDate today) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            sb.append(d).append("(").append(DAY_LABELS[i]).append(")");
            if (d.equals(today)) sb.append("←오늘");
            if (i < 6) sb.append(" ");
        }
        return sb.toString();
    }

    private Optional<DateRange> parseResponse(String raw) throws Exception {
        String json = raw.replaceAll("(?s)```json\\s*", "").replaceAll("```", "").trim();
        JsonNode node = objectMapper.readTree(json);

        if (node.get("start").isNull()) {
            return Optional.empty();
        }

        LocalDateTime start = LocalDateTime.parse(node.get("start").asText());
        LocalDateTime end = LocalDateTime.parse(node.get("end").asText());
        return Optional.of(new DateRange(start, end));
    }
}
