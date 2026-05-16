package com.project.backend.domain.chat.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Map;

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

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd (E) HH:mm", Locale.KOREAN);
    private static final String[] DAY_LABELS = {"월", "화", "수", "목", "금", "토", "일"};

    private static final String[] DAY_FULL_LABELS = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};

    private String buildWeekContext(LocalDate today) {
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextMonday = thisMonday.plusWeeks(1);

        StringBuilder sb = new StringBuilder();
        sb.append("이번 주: ").append(formatWeekDays(thisMonday, today)).append("\n");
        sb.append("다음 주: ").append(formatWeekDays(nextMonday, today)).append("\n");

        sb.append("오늘=").append(today);
        sb.append(", 내일=").append(today.plusDays(1));
        sb.append(", 모레=").append(today.plusDays(2)).append("\n");

        sb.append("이번주 날짜:\n");
        for (int i = 0; i < 7; i++) {
            sb.append("  이번주 ").append(DAY_FULL_LABELS[i]).append(" = ").append(thisMonday.plusDays(i)).append("\n");
        }
        sb.append("다음주 날짜:\n");
        for (int i = 0; i < 7; i++) {
            sb.append("  다음주 ").append(DAY_FULL_LABELS[i]).append(" = ").append(nextMonday.plusDays(i)).append("\n");
        }
        return sb.toString();
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

    private static final String[] DAY_NAMES = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};

    public String replaceRelativeDates(String message) {
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextMonday = thisMonday.plusWeeks(1);

        String result = message;
        for (int i = 0; i < 7; i++) {
            String day = DAY_NAMES[i];
            String nextDate = nextMonday.plusDays(i).toString();
            String thisDate = thisMonday.plusDays(i).toString();
            result = result.replace("다음 주 " + day, nextDate + "(다음주 " + day + ")");
            result = result.replace("다음주 " + day, nextDate + "(다음주 " + day + ")");
            result = result.replace("이번 주 " + day, thisDate + "(이번주 " + day + ")");
            result = result.replace("이번주 " + day, thisDate + "(이번주 " + day + ")");
        }
        return result;
    }

    public String getSystemPrompt() {
        return getSystemPrompt(null, null);
    }

    public String getSystemPrompt(String scheduleContext) {
        return getSystemPrompt(scheduleContext, null, null);
    }

    public String getSystemPrompt(String scheduleContext, Map<String, String> pendingCtx) {
        return getSystemPrompt(scheduleContext, pendingCtx, null);
    }

    public String getSystemPrompt(String scheduleContext, Map<String, String> pendingCtx, Map<String, String> lastActionCtx) {
        String contextBlock = scheduleContext != null
                ? "[사용자의 일정 및 할 일]\n" + scheduleContext + "\n\n위 내용을 참고하여 답변하세요."
                : "[사용자의 일정 및 할 일]\n조회된 일정 및 할 일이 없어요.\n\n일정·할 일에 관한 질문이라면 '관련 일정이나 할 일을 찾지 못했어요'라고 답변하세요.";

        if (pendingCtx != null) {
            String pendingBlock = "\n\n[진행 중인 작업]\n"
                    + "반복 일정/할 일 ID:" + pendingCtx.get("scheduleId")
                    + " (TYPE:" + pendingCtx.get("scheduleType") + ")의 수정/삭제 범위를 사용자에게 이미 물었어요.\n"
                    + "이번 메시지에 '이번만', '이후 전체', '전체', '모두' 등 범위 응답이 있으면 "
                    + "scheduleId=" + pendingCtx.get("scheduleId")
                    + ", scheduleType=" + pendingCtx.get("scheduleType")
                    + ", scope를 포함하여 즉시 수정/삭제를 실행해요. 범위를 다시 묻지 마세요.";
            contextBlock += pendingBlock;
        } else if (lastActionCtx != null) {
            // pendingCtx가 없을 때만 적용 — 직전 턴에서 처리한 일정 ID를 힌트로 제공
            String lastActionBlock = "\n\n[직전 처리 일정 — 최우선 참조]\n"
                    + "직전 턴에서 ID:" + lastActionCtx.get("scheduleId")
                    + ", TYPE:" + lastActionCtx.get("scheduleType")
                    + " 일정을 처리했어요.\n"
                    + "이번 메시지가 '그거', '그 일정', '방금 것', '그냥 삭제', '그냥 수정', '그냥 ~' 등 "
                    + "직전 일정을 가리키는 표현이면, 대화 기록에 다른 일정이 언급되어 있더라도 반드시 "
                    + "scheduleId=" + lastActionCtx.get("scheduleId")
                    + ", scheduleType=" + lastActionCtx.get("scheduleType")
                    + "을(를) 사용해요. 대화 기록의 다른 ID를 추측하거나 사용하지 마세요.";
            contextBlock += lastActionBlock;
        }

        String weekContext = buildWeekContext(LocalDate.now());
        log.debug("week_context: {}", weekContext);

        return systemPrompt
                .replace("{current_date_time}", LocalDateTime.now().format(DATE_TIME_FORMATTER))
                .replace("{week_context}", weekContext)
                .replace("{schedule_context}", contextBlock);
    }
}
