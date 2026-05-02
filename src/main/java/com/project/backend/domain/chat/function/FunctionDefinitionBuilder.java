package com.project.backend.domain.chat.function;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FunctionDefinitionBuilder {

    private static final String[] DAY_LABELS = {"월", "화", "수", "목", "금", "토", "일"};

    public List<Map<String, Object>> build() {
        return build(LocalDate.now());
    }

    public List<Map<String, Object>> build(LocalDate today) {
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate nextMonday = thisMonday.plusWeeks(1);

        StringBuilder dateRef = new StringBuilder("날짜 직접 계산 금지 — 아래 값을 그대로 사용: 오늘=").append(today);
        dateRef.append(", 내일=").append(today.plusDays(1));
        for (int i = 0; i < 7; i++) {
            dateRef.append(", 이번주").append(DAY_LABELS[i]).append("요일=").append(thisMonday.plusDays(i));
        }
        for (int i = 0; i < 7; i++) {
            dateRef.append(", 다음주").append(DAY_LABELS[i]).append("요일=").append(nextMonday.plusDays(i));
        }
        String dateHint = dateRef.toString();

        return List.of(
                buildCreateSchedule(dateHint),
                buildUpdateSchedule(dateHint),
                buildDeleteSchedule(),
                buildAskForClarification(),
                buildRespondToUser()
        );
    }

    // createSchedule
    private Map<String, Object> buildCreateSchedule(String dateHint) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("scheduleType",          enumProp("일정 유형 — '몇 시에', '오전/오후 N시' 등 구체적 시간이 명시된 약속·미팅·회의면 EVENT; 날짜만 있거나 시간 없이 할 일·체크리스트 성격이면 반드시 TODO", "EVENT", "TODO"));
        props.put("title",                 strProp("제목"));
        props.put("isRecurring",           boolProp("반복 여부"));
        props.put("startTime",             strProp("시작 시간 ISO-8601 (EVENT 필수). " + dateHint));
        props.put("endTime",               strProp("종료 시간 ISO-8601 (EVENT 필수)"));
        props.put("startDate",             strProp("시작 날짜 yyyy-MM-dd (TODO 필수, 반복TODO는 첫 발생일). " + dateHint));
        props.put("dueTime",               strProp("할 일 수행 시각 HH:mm (TODO 전용, '오후 7시' 등 시간이 언급된 경우 반드시 설정)"));
        props.put("location",              strProp("장소 (EVENT 선택)"));
        props.put("isAllDay",              boolProp("종일 여부"));
        props.put("priority",              enumProp("우선순위 (TODO 전용)", "HIGH", "MEDIUM", "LOW"));
        props.put("recurrenceType",        enumProp("반복 주기", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"));
        props.put("recurrenceInterval",    intProp("반복 간격 (매주=1, 격주=2)"));
        props.put("recurrenceDaysOfWeek",  arrayEnumProp("반복 요일 (WEEKLY 전용)",
                "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"));
        props.put("recurrenceEndType",     enumProp("종료 조건", "NEVER", "END_BY_DATE", "END_BY_COUNT"));
        props.put("recurrenceEndDate",     strProp("반복 종료 날짜 yyyy-MM-dd (END_BY_DATE 전용)"));
        props.put("recurrenceCount",       intProp("반복 횟수 (END_BY_COUNT 전용)"));

        return wrapFunction("createSchedule",
                "새 일정(EVENT) 또는 할 일(TODO)을 등록한다. '~시', '~분' 등 특정 시간이 언급되고 일정·미팅·약속·회의 성격이면 EVENT, 할 일·체크리스트·루틴 성격이면 TODO로 등록하고 해당 시간을 dueTime에 설정한다. 반복이면 isRecurring=true와 recurrenceType을 반드시 포함한다.",
                props,
                List.of("scheduleType", "title", "isRecurring"));
    }

    // updateSchedule
    private Map<String, Object> buildUpdateSchedule(String dateHint) {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("scheduleId",     intProp("수정할 ID — 컨텍스트 [ID:N] 값 사용"));
        props.put("scheduleType",   enumProp("일정 유형", "EVENT", "TODO"));
        // [핵심] occurrenceDate: 반복 일정의 해당 회차를 특정하기 위한 날짜
        props.put("occurrenceDate", strProp("반복 일정 해당 회차 날짜 yyyy-MM-dd (반복일정만). " + dateHint));
        props.put("title",          strProp("새 제목"));
        props.put("startTime",      strProp("새 시작 시간 ISO-8601 (EVENT 전용). " + dateHint));
        props.put("endTime",        strProp("새 종료 시간 ISO-8601 (EVENT 전용)"));
        props.put("startDate",      strProp("새 시작 날짜 yyyy-MM-dd (TODO 전용). " + dateHint));
        props.put("dueTime",        strProp("새 마감 시간 HH:mm (TODO 전용)"));
        props.put("location",       strProp("새 장소 (EVENT 전용)"));
        props.put("isAllDay",       boolProp("종일 여부"));
        props.put("priority",       enumProp("우선순위 (TODO 전용)", "HIGH", "MEDIUM", "LOW"));
        // [핵심] scope: ALL 없음 — 실제 enum에 없으므로
        props.put("scope",          enumProp("사용자가 '이번만', '이후 전체' 등 범위를 직접 언급한 경우에만 포함. 명시하지 않았으면 절대 포함하지 말 것", "THIS_ONLY", "THIS_AND_AFTER"));

        return wrapFunction("updateSchedule",
                "기존 일정 또는 할 일을 수정한다. 반복 일정인데 사용자가 범위를 언급하지 않았으면 반드시 먼저 askForClarification으로 범위를 확인해야 한다. scope는 사용자가 직접 명시한 경우에만 포함한다.",
                props,
                List.of("scheduleId", "scheduleType"));
    }

    // deleteSchedule
    private Map<String, Object> buildDeleteSchedule() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("scheduleId",     intProp("삭제할 ID — 컨텍스트 [ID:N] 값 사용"));
        props.put("scheduleType",   enumProp("일정 유형", "EVENT", "TODO"));
        props.put("occurrenceDate", strProp("반복 일정 해당 회차 날짜 yyyy-MM-dd (반복일정만)"));
        props.put("scope",          enumProp("사용자가 '이번만', '이후 전체' 등 범위를 직접 언급한 경우에만 포함. 명시하지 않았으면 절대 포함하지 말 것", "THIS_ONLY", "THIS_AND_AFTER"));

        return wrapFunction("deleteSchedule",
                "일정 또는 할 일을 삭제한다. 반복 일정인데 사용자가 범위를 언급하지 않았으면 반드시 먼저 askForClarification으로 범위를 확인해야 한다. scope는 사용자가 직접 명시한 경우에만 포함한다.",
                props,
                List.of("scheduleId", "scheduleType"));
    }

    // askForClarification
    // [핵심] 텍스트로 되묻지 않고 명시적 함수로 정의하는 이유:
    // → 백엔드가 "되묻는 중"인지 "일반 답변"인지 구분 가능
    // → 프론트가 action: CLARIFYING으로 별도 UI 처리 가능
    private Map<String, Object> buildAskForClarification() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("question",      strProp("사용자에게 보여줄 질문"));
        props.put("scheduleId",    intProp("대상 일정/할 일의 ID (컨텍스트 [ID:N] 값, 특정 가능한 경우)"));
        props.put("scheduleType",  enumProp("대상 유형 (특정 가능한 경우)", "EVENT", "TODO"));

        return wrapFunction("askForClarification",
                "요청이 애매하거나 반복 일정 수정/삭제 범위 확인이 필요할 때 사용자에게 되묻는다.",
                props,
                List.of("question"));
    }

    // respondToUser
    // [핵심] tool_choice:"required" 환경에서 일반 텍스트 응답(조회 안내, 일반 대화 등)을 함수 형태로 반환
    private Map<String, Object> buildRespondToUser() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("message", strProp("사용자에게 전달할 텍스트 응답"));
        return wrapFunction("respondToUser",
                "일정 CRUD나 되묻기가 아닌 경우 — 일반 대화, 질문 답변, 조회 결과 안내 등 텍스트로만 응답할 때 사용한다.",
                props,
                List.of("message"));
    }

    // OpenAI tools 포맷 래핑
    private Map<String, Object> wrapFunction(
            String name, String description,
            Map<String, Object> properties, List<String> required) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", Map.of(
                                "type", "object",
                                "properties", properties,
                                "required", required
                        )
                )
        );
    }

    // 파라미터 타입 헬퍼
    private Map<String, Object> strProp(String desc) {
        return Map.of("type", "string", "description", desc);
    }

    private Map<String, Object> boolProp(String desc) {
        return Map.of("type", "boolean", "description", desc);
    }

    private Map<String, Object> intProp(String desc) {
        return Map.of("type", "integer", "description", desc);
    }

    private Map<String, Object> enumProp(String desc, String... values) {
        return Map.of("type", "string", "description", desc, "enum", List.of(values));
    }

    private Map<String, Object> arrayEnumProp(String desc, String... values) {
        return Map.of(
                "type", "array",
                "description", desc,
                "items", Map.of("type", "string", "enum", List.of(values))
        );
    }
}
