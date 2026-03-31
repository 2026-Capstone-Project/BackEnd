package com.project.backend.domain.chat.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.domain.chat.enums.ActionType;
import com.project.backend.domain.chat.enums.ScheduleType;
import com.project.backend.domain.common.recurrence.enums.RecurrenceEndType;
import com.project.backend.domain.common.recurrence.enums.RecurrenceFrequency;
import com.project.backend.domain.event.dto.request.EventReqDTO;
import com.project.backend.domain.event.dto.request.RecurrenceGroupReqDTO;
import com.project.backend.domain.event.dto.response.EventResDTO;
import com.project.backend.domain.event.enums.RecurrenceUpdateScope;
import com.project.backend.domain.event.service.command.EventCommandService;
import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.enums.Priority;
import com.project.backend.domain.todo.service.command.TodoCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FunctionCallHandler {

    private final EventCommandService eventCommandService;
    private final TodoCommandService todoCommandService;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────
    // 진입점
    // ─────────────────────────────────────────────────────
    public ScheduleActionResult handle(String functionName, String argsJson, Long memberId) {
        log.debug("FunctionCallHandler - function: {}, memberId: {}", functionName, memberId);
        Map<String, Object> args = parseArgs(argsJson);

        return switch (functionName) {
            case "createSchedule" -> handleCreate(args, memberId);
            case "updateSchedule" -> handleUpdate(args, memberId);
            case "deleteSchedule" -> handleDelete(args, memberId);
            default -> throw new IllegalArgumentException("Unknown function: " + functionName);
        };
    }

    // ─────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────
    private ScheduleActionResult handleCreate(Map<String, Object> args, Long memberId) {
        String scheduleType = (String) args.get("scheduleType");

        if ("EVENT".equals(scheduleType)) return createEvent(args, memberId);
        if ("TODO".equals(scheduleType))  return createTodo(args, memberId);
        throw new IllegalArgumentException("Unknown scheduleType: " + scheduleType);
    }

    private ScheduleActionResult createEvent(Map<String, Object> args, Long memberId) {
        boolean isRecurring = Boolean.TRUE.equals(args.get("isRecurring"));

        LocalDateTime startTime = parseDateTime((String) args.get("startTime"));
        LocalDateTime endTime   = parseDateTime((String) args.get("endTime"));

        EventReqDTO.CreateReq req = EventReqDTO.CreateReq.builder()
                .title((String) args.get("title"))
                .startTime(startTime)
                .endTime(endTime)
                .location((String) args.get("location"))
                .isAllDay(args.get("isAllDay") != null ? (Boolean) args.get("isAllDay") : false)
                .recurrenceGroup(isRecurring ? buildEventRecurrenceGroup(args) : null)
                .build();

        EventResDTO.CreateRes res = eventCommandService.createEvent(req, memberId);

        return new ScheduleActionResult(
                ActionType.CREATED, ScheduleType.EVENT,
                res.id(), res.recurrenceGroupId(),
                buildSummary("일정", (String) args.get("title"), startTime.toLocalDate())
        );
    }

    private ScheduleActionResult createTodo(Map<String, Object> args, Long memberId) {
        boolean isRecurring = Boolean.TRUE.equals(args.get("isRecurring"));

        LocalDate startDate = parseDate((String) args.get("startDate"));
        LocalTime dueTime   = args.get("dueTime") != null
                ? LocalTime.parse((String) args.get("dueTime")) : null;
        Priority priority   = args.get("priority") != null
                ? Priority.valueOf((String) args.get("priority")) : Priority.MEDIUM;

        TodoReqDTO.CreateTodo req = TodoReqDTO.CreateTodo.builder()
                .title((String) args.get("title"))
                .startDate(startDate)
                .dueTime(dueTime)
                .isAllDay(args.get("isAllDay") != null ? (Boolean) args.get("isAllDay") : false)
                .priority(priority)
                .recurrenceGroup(isRecurring ? buildTodoRecurrenceGroup(args) : null)
                .build();

        TodoResDTO.TodoInfo res = todoCommandService.createTodo(memberId, req);

        return new ScheduleActionResult(
                ActionType.CREATED, ScheduleType.TODO,
                res.todoId(), res.recurrenceGroupId(),
                buildSummary("할 일", (String) args.get("title"), startDate)
        );
    }

    // ─────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────
    private ScheduleActionResult handleUpdate(Map<String, Object> args, Long memberId) {
        Long scheduleId     = getLong(args, "scheduleId");
        String scheduleType = (String) args.get("scheduleType");

        if ("EVENT".equals(scheduleType)) return updateEvent(scheduleId, args, memberId);
        if ("TODO".equals(scheduleType))  return updateTodo(scheduleId, args, memberId);
        throw new IllegalArgumentException("Unknown scheduleType: " + scheduleType);
    }

    private ScheduleActionResult updateEvent(Long eventId, Map<String, Object> args, Long memberId) {
        // scope null 시 THIS_EVENT 기본값 — 단순 일정은 scope 무관
        RecurrenceUpdateScope scope = mapEventScope((String) args.get("scope"));

        LocalDateTime occurrenceDate = args.get("occurrenceDate") != null
                ? parseDate((String) args.get("occurrenceDate")).atStartOfDay() : null;
        LocalDateTime startTime = args.get("startTime") != null
                ? parseDateTime((String) args.get("startTime")) : null;
        LocalDateTime endTime   = args.get("endTime") != null
                ? parseDateTime((String) args.get("endTime")) : null;

        // EventReqDTO.UpdateReq는 @Builder 있음
        EventReqDTO.UpdateReq req = EventReqDTO.UpdateReq.builder()
                .title((String) args.get("title"))
                .startTime(startTime)
                .endTime(endTime)
                .location((String) args.get("location"))
                .isAllDay((Boolean) args.get("isAllDay"))
                .build();

        eventCommandService.updateEvent(req, eventId, memberId, scope, occurrenceDate);

        return new ScheduleActionResult(
                ActionType.UPDATED, ScheduleType.EVENT, eventId, null, "일정이 수정되었어요."
        );
    }

    private ScheduleActionResult updateTodo(Long todoId, Map<String, Object> args, Long memberId) {
        // Todo enum과 Event enum 이름 충돌 → fully qualified name 사용
        com.project.backend.domain.todo.enums.RecurrenceUpdateScope scope = mapTodoScope((String) args.get("scope"));

        LocalDate occurrenceDate = args.get("occurrenceDate") != null
                ? parseDate((String) args.get("occurrenceDate")) : null;
        LocalDate startDate      = args.get("startDate") != null
                ? parseDate((String) args.get("startDate")) : null;
        LocalTime dueTime        = args.get("dueTime") != null
                ? LocalTime.parse((String) args.get("dueTime")) : null;
        Priority priority        = args.get("priority") != null
                ? Priority.valueOf((String) args.get("priority")) : null;

        // TodoReqDTO.UpdateTodo는 @Builder 없는 record → 생성자 직접 사용
        TodoReqDTO.UpdateTodo req = new TodoReqDTO.UpdateTodo(
                (String) args.get("title"),
                startDate,
                null,      // endDate
                dueTime,
                (Boolean) args.get("isAllDay"),
                priority,
                null,      // color
                null,      // memo
                null       // recurrenceGroup
        );

        TodoResDTO.TodoInfo res = todoCommandService.updateTodo(memberId, todoId, occurrenceDate, scope, req);

        return new ScheduleActionResult(
                ActionType.UPDATED, ScheduleType.TODO, todoId, res.recurrenceGroupId(), "할 일이 수정되었어요."
        );
    }

    // ─────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────
    private ScheduleActionResult handleDelete(Map<String, Object> args, Long memberId) {
        Long scheduleId     = getLong(args, "scheduleId");
        String scheduleType = (String) args.get("scheduleType");

        if ("EVENT".equals(scheduleType)) return deleteEvent(scheduleId, args, memberId);
        if ("TODO".equals(scheduleType))  return deleteTodo(scheduleId, args, memberId);
        throw new IllegalArgumentException("Unknown scheduleType: " + scheduleType);
    }

    private ScheduleActionResult deleteEvent(Long eventId, Map<String, Object> args, Long memberId) {
        RecurrenceUpdateScope scope = mapEventScope((String) args.get("scope"));
        LocalDateTime occurrenceDate = args.get("occurrenceDate") != null
                ? parseDate((String) args.get("occurrenceDate")).atStartOfDay() : null;

        eventCommandService.deleteEvent(eventId, occurrenceDate, scope, memberId);

        return new ScheduleActionResult(
                ActionType.DELETED, ScheduleType.EVENT, eventId, null, "일정이 삭제되었어요."
        );
    }

    private ScheduleActionResult deleteTodo(Long todoId, Map<String, Object> args, Long memberId) {
        com.project.backend.domain.todo.enums.RecurrenceUpdateScope scope = mapTodoScope((String) args.get("scope"));
        LocalDate occurrenceDate = args.get("occurrenceDate") != null
                ? parseDate((String) args.get("occurrenceDate")) : null;

        todoCommandService.deleteTodo(memberId, todoId, occurrenceDate, scope);

        return new ScheduleActionResult(
                ActionType.DELETED, ScheduleType.TODO, todoId, null, "할 일이 삭제되었어요."
        );
    }

    // ─────────────────────────────────────────────────────
    // RecurrenceGroup DTO 빌드
    // ─────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private RecurrenceGroupReqDTO.CreateReq buildEventRecurrenceGroup(Map<String, Object> args) {
        RecurrenceFrequency frequency = RecurrenceFrequency.valueOf(
                (String) args.getOrDefault("recurrenceType", "WEEKLY"));
        RecurrenceEndType endType = args.get("recurrenceEndType") != null
                ? RecurrenceEndType.valueOf((String) args.get("recurrenceEndType"))
                : RecurrenceEndType.NEVER;
        LocalDate endDate = args.get("recurrenceEndDate") != null
                ? parseDate((String) args.get("recurrenceEndDate")) : null;
        Integer count = args.get("recurrenceCount") != null
                ? ((Number) args.get("recurrenceCount")).intValue() : null;

        // LLM이 ["MONDAY", "FRIDAY"] 형태로 반환 → DayOfWeek enum 리스트로 변환
        List<DayOfWeek> daysOfWeek = null;
        if (args.get("recurrenceDaysOfWeek") instanceof List<?> rawList) {
            daysOfWeek = rawList.stream()
                    .map(d -> DayOfWeek.valueOf((String) d))
                    .toList();
        }

        // RecurrenceGroupReqDTO.CreateReq는 @Builder 있음
        return RecurrenceGroupReqDTO.CreateReq.builder()
                .frequency(frequency)
                .daysOfWeek(daysOfWeek)
                .endType(endType)
                .endDate(endDate)
                .occurrenceCount(count)
                .build();
        // intervalValue는 null로 둠 — getIntervalOrDefault()가 항상 1을 반환하므로
        // 격주 기능은 서비스 레벨에서 지원 시 수정 필요
    }

    @SuppressWarnings("unchecked")
    private TodoReqDTO.RecurrenceGroupReq buildTodoRecurrenceGroup(Map<String, Object> args) {
        RecurrenceFrequency frequency = RecurrenceFrequency.valueOf(
                (String) args.getOrDefault("recurrenceType", "WEEKLY"));
        RecurrenceEndType endType = args.get("recurrenceEndType") != null
                ? RecurrenceEndType.valueOf((String) args.get("recurrenceEndType"))
                : RecurrenceEndType.NEVER;
        LocalDate endDate = args.get("recurrenceEndDate") != null
                ? parseDate((String) args.get("recurrenceEndDate")) : null;
        Integer count = args.get("recurrenceCount") != null
                ? ((Number) args.get("recurrenceCount")).intValue() : null;

        List<DayOfWeek> daysOfWeek = null;
        if (args.get("recurrenceDaysOfWeek") instanceof List<?> rawList) {
            daysOfWeek = rawList.stream()
                    .map(d -> DayOfWeek.valueOf((String) d))
                    .toList();
        }

        return new TodoReqDTO.RecurrenceGroupReq(
                frequency, null, daysOfWeek, null, null, null, null, null, endType, endDate, count
        );
    }

    // ─────────────────────────────────────────────────────
    // Scope 매핑 — LLM 값 → 기존 코드 enum
    // ─────────────────────────────────────────────────────
    private RecurrenceUpdateScope mapEventScope(String scope) {
        if (scope == null) return RecurrenceUpdateScope.THIS_EVENT;
        return switch (scope) {
            case "THIS_ONLY"      -> RecurrenceUpdateScope.THIS_EVENT;
            case "THIS_AND_AFTER" -> RecurrenceUpdateScope.THIS_AND_FOLLOWING_EVENTS;
            default -> throw new IllegalArgumentException("Unknown scope: " + scope);
        };
    }

    private com.project.backend.domain.todo.enums.RecurrenceUpdateScope mapTodoScope(String scope) {
        if (scope == null) return com.project.backend.domain.todo.enums.RecurrenceUpdateScope.THIS_TODO;
        return switch (scope) {
            case "THIS_ONLY"      -> com.project.backend.domain.todo.enums.RecurrenceUpdateScope.THIS_TODO;
            case "THIS_AND_AFTER" -> com.project.backend.domain.todo.enums.RecurrenceUpdateScope.THIS_AND_FOLLOWING;
            default -> throw new IllegalArgumentException("Unknown scope: " + scope);
        };
    }

    // ─────────────────────────────────────────────────────
    // 파싱 헬퍼
    // ─────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArgs(String argsJson) {
        try {
            return objectMapper.readValue(argsJson, Map.class);
        } catch (Exception e) {
            log.error("Function args 파싱 실패: {}", argsJson, e);
            throw new IllegalArgumentException("Invalid function arguments: " + argsJson);
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null) return null;
        return LocalDateTime.parse(value);
    }

    private LocalDate parseDate(String value) {
        if (value == null) return null;
        // LLM이 "2026-03-27T00:00:00" 또는 "2026-03-27" 두 형식 모두 반환 가능
        return value.contains("T")
                ? LocalDateTime.parse(value).toLocalDate()
                : LocalDate.parse(value);
    }

    private Long getLong(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value instanceof Number n) return n.longValue();
        throw new IllegalArgumentException("Missing or invalid field: " + key);
    }

    private String buildSummary(String type, String title, LocalDate date) {
        return String.format("'%s' %s이(가) %d월 %d일에 등록되었어요.",
                title, type, date.getMonthValue(), date.getDayOfMonth());
    }
}