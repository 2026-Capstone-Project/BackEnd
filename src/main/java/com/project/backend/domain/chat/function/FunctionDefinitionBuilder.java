package com.project.backend.domain.chat.function;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FunctionDefinitionBuilder {

    public List<Map<String, Object>> build() {
        return List.of(
                buildCreateSchedule(),
                buildUpdateSchedule(),
                buildDeleteSchedule(),
                buildAskForClarification()
        );
    }

    // createSchedule
    private Map<String, Object> buildCreateSchedule() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("scheduleType",          enumProp("일정 유형", "EVENT", "TODO"));
        props.put("title",                 strProp("제목"));
        props.put("isRecurring",           boolProp("반복 여부"));
        props.put("startTime",             strProp("시작 시간 ISO-8601 (EVENT 필수): 2026-03-27T10:00:00"));
        props.put("endTime",               strProp("종료 시간 ISO-8601 (EVENT 필수)"));
        props.put("startDate",             strProp("시작 날짜 yyyy-MM-dd (TODO 필수)"));
        props.put("dueTime",               strProp("마감 시간 HH:mm (TODO 선택)"));
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
                "새 일정 또는 할 일을 등록한다. 반복 일정이면 isRecurring=true와 recurrenceType을 반드시 포함한다.",
                props,
                List.of("scheduleType", "title", "isRecurring"));
    }

    // updateSchedule
    private Map<String, Object> buildUpdateSchedule() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("scheduleId",     intProp("수정할 ID — 컨텍스트 [ID:N] 값 사용"));
        props.put("scheduleType",   enumProp("일정 유형", "EVENT", "TODO"));
        // [핵심] occurrenceDate: 반복 일정의 해당 회차를 특정하기 위한 날짜
        props.put("occurrenceDate", strProp("반복 일정 해당 회차 날짜 yyyy-MM-dd (반복일정만)"));
        props.put("title",          strProp("새 제목"));
        props.put("startTime",      strProp("새 시작 시간 ISO-8601 (EVENT 전용)"));
        props.put("endTime",        strProp("새 종료 시간 ISO-8601 (EVENT 전용)"));
        props.put("startDate",      strProp("새 시작 날짜 yyyy-MM-dd (TODO 전용)"));
        props.put("dueTime",        strProp("새 마감 시간 HH:mm (TODO 전용)"));
        props.put("location",       strProp("새 장소 (EVENT 전용)"));
        props.put("isAllDay",       boolProp("종일 여부"));
        props.put("priority",       enumProp("우선순위 (TODO 전용)", "HIGH", "MEDIUM", "LOW"));
        // [핵심] scope: ALL 없음 — 실제 enum에 없으므로
        props.put("scope",          enumProp("반복 일정 수정 범위 (반복일정만)", "THIS_ONLY", "THIS_AND_AFTER"));

        return wrapFunction("updateSchedule",
                "기존 일정 또는 할 일을 수정한다. 반복 일정이면 scope를 반드시 포함한다.",
                props,
                List.of("scheduleId", "scheduleType"));
    }

    // deleteSchedule
    private Map<String, Object> buildDeleteSchedule() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("scheduleId",     intProp("삭제할 ID — 컨텍스트 [ID:N] 값 사용"));
        props.put("scheduleType",   enumProp("일정 유형", "EVENT", "TODO"));
        props.put("occurrenceDate", strProp("반복 일정 해당 회차 날짜 yyyy-MM-dd (반복일정만)"));
        props.put("scope",          enumProp("반복 일정 삭제 범위 (반복일정만)", "THIS_ONLY", "THIS_AND_AFTER"));

        return wrapFunction("deleteSchedule",
                "일정 또는 할 일을 삭제한다. 반복 일정이면 scope를 반드시 포함한다.",
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
