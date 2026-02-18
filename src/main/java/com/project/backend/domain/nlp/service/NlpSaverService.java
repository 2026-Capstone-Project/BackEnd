package com.project.backend.domain.nlp.service;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceGroup;
import com.project.backend.domain.event.enums.RecurrenceEndType;
import com.project.backend.domain.event.enums.RecurrenceFrequency;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.repository.RecurrenceGroupRepository;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.exception.NlpErrorCode;
import com.project.backend.domain.nlp.exception.NlpException;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import com.project.backend.domain.todo.repository.TodoRecurrenceGroupRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlpSaverService {

    private final EventRepository eventRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final TodoRepository todoRepository;
    private final TodoRecurrenceGroupRepository todoRecurrenceGroupRepository;

    /**
     * 파싱된 항목을 저장하고 생성된 ID를 반환한다.
     */
    public Long save(NlpReqDTO.ConfirmItem item, Member member) {
        return switch (item.type()) {
            case EVENT -> saveEvent(item, member);
            case TODO -> saveTodo(item, member);
            default -> throw new NlpException(NlpErrorCode.INVALID_ITEM_TYPE);
        };
    }

    // ==================== Event 저장 ====================

    private Long saveEvent(NlpReqDTO.ConfirmItem item, Member member) {
        if (item.isRecurring() && item.recurrenceRule() != null) {
            return saveRecurringEvent(item, member);
        } else {
            return saveSingleEvent(item, member);
        }
    }

    /**
     * 단일 일정 저장
     */
    private Long saveSingleEvent(NlpReqDTO.ConfirmItem item, Member member) {
        TimeRange timeRange = calculateTimeRange(item, item.date());

        Event event = Event.createSingle(
                member,
                item.title(),
                timeRange.startTime(),
                timeRange.endTime(),
                item.durationMinutes(),
                item.isAllDay(),
                item.getColorOrDefault()
        );

        eventRepository.save(event);
        return event.getId();
    }

    /**
     * 반복 일정 저장
     * - 1개 Event + 1개 RecurrenceGroup 생성
     * - 조회 시 Generator 패턴으로 반복 인스턴스를 동적 생성
     */
    private Long saveRecurringEvent(NlpReqDTO.ConfirmItem item, Member member) {
        NlpReqDTO.RecurrenceRule rule = item.recurrenceRule();
        TimeRange timeRange = calculateTimeRange(item, item.date());

        // 1. RecurrenceGroup 생성
        RecurrenceGroup group = createRecurrenceGroup(member, rule);
        recurrenceGroupRepository.save(group);

        // 2. Event 생성 (1개만)
        Event event = Event.createRecurring(
                member,
                item.title(),
                timeRange.startTime(),
                timeRange.endTime(),
                item.durationMinutes(),
                item.isAllDay(),
                rule.frequency(),
                group,
                item.getColorOrDefault()
        );
        eventRepository.save(event);

        // 3. 양방향 연관관계 설정
        group.setEvent(event);

        log.debug("반복 일정 생성 완료 - eventId: {}, groupId: {}", event.getId(), group.getId());
        return event.getId();
    }

    private RecurrenceGroup createRecurrenceGroup(Member member, NlpReqDTO.RecurrenceRule rule) {
        return RecurrenceGroup.create(
                member,
                rule.frequency(),
                rule.getIntervalOrDefault(),
                joinDaysOfWeek(rule.daysOfWeek()),
                rule.monthlyType(),
                joinDaysOfMonth(rule.daysOfMonth()),
                rule.weekOfMonth(),
                rule.dayOfWeekInMonth(),
                rule.monthOfYear(),
                rule.getEndTypeOrDefault(),
                rule.endDate(),
                rule.occurrenceCount(),
                1  // createdCount: 항상 1 (단일 Event 생성)
        );
    }

    // ==================== Todo 저장 ====================

    private Long saveTodo(NlpReqDTO.ConfirmItem item, Member member) {
        if (item.isRecurring() && item.recurrenceRule() != null) {
            return saveRecurringTodo(item, member);
        } else {
            return saveSingleTodo(item, member);
        }
    }

    /**
     * 단일 할 일 저장
     */
    private Long saveSingleTodo(NlpReqDTO.ConfirmItem item, Member member) {
        LocalDate startDate = item.date();
        LocalTime dueTime = item.getStartTimeOrDefault();

        Todo todo = Todo.createSingle(
                member,
                item.title(),
                startDate,
                dueTime,
                item.isAllDay(),
                null,  // priority: NLP에서 파싱하지 않음, 기본값 사용
                null,  // color: 기본값 사용
                null   // memo
        );

        todoRepository.save(todo);
        return todo.getId();
    }

    /**
     * 반복 할 일 저장
     * - 1개 Todo + 1개 TodoRecurrenceGroup 생성
     * - 조회 시 Generator 패턴으로 반복 인스턴스를 동적 생성
     */
    private Long saveRecurringTodo(NlpReqDTO.ConfirmItem item, Member member) {
        NlpReqDTO.RecurrenceRule rule = item.recurrenceRule();
        LocalDate startDate = item.date();
        LocalTime dueTime = item.getStartTimeOrDefault();

        // 1. TodoRecurrenceGroup 생성
        TodoRecurrenceGroup group = createTodoRecurrenceGroup(member, rule);
        todoRecurrenceGroupRepository.save(group);

        // 2. Todo 생성 (1개만)
        Todo todo = Todo.createRecurring(
                member,
                item.title(),
                startDate,
                dueTime,
                item.isAllDay(),
                null,  // priority
                null,  // color
                null,  // memo
                group
        );
        todoRepository.save(todo);

        // 3. 양방향 연관관계 설정
        group.setTodo(todo);

        log.debug("반복 할 일 생성 완료 - todoId: {}, groupId: {}", todo.getId(), group.getId());
        return todo.getId();
    }

    private TodoRecurrenceGroup createTodoRecurrenceGroup(Member member, NlpReqDTO.RecurrenceRule rule) {
        return TodoRecurrenceGroup.create(
                member,
                rule.frequency(),
                rule.getIntervalOrDefault(),
                joinDaysOfWeek(rule.daysOfWeek()),
                rule.monthlyType(),
                joinDaysOfMonth(rule.daysOfMonth()),
                rule.weekOfMonth(),
                rule.dayOfWeekInMonth(),
                rule.getEndTypeOrDefault(),
                rule.endDate(),
                rule.occurrenceCount()
        );
    }

    // ==================== 헬퍼 메서드 ====================

    private TimeRange calculateTimeRange(NlpReqDTO.ConfirmItem item, LocalDate date) {
        if (item.isAllDay()) {
            return new TimeRange(date.atStartOfDay(), date.atTime(23, 59, 59));
        }

        LocalTime startTime = item.getStartTimeOrDefault() != null ? item.getStartTimeOrDefault() : LocalTime.of(9, 0);
        LocalDateTime start = LocalDateTime.of(date, startTime);

        LocalDateTime end;
        if (item.endTime() != null) {
            end = LocalDateTime.of(date, item.endTime());
        } else if (item.durationMinutes() != null && item.durationMinutes() > 0) {
            end = start.plusMinutes(item.durationMinutes());
        } else {
            // endTime, durationMinutes 둘 다 없으면 기본 1시간
            end = start.plusHours(1);
        }

        return new TimeRange(start, end);
    }

    /**
     * 요일 리스트를 콤마 구분 문자열로 변환
     * ["MONDAY", "WEDNESDAY", "FRIDAY"] → "MONDAY,WEDNESDAY,FRIDAY"
     */
    private String joinDaysOfWeek(List<String> daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return null;
        }
        return String.join(",", daysOfWeek);
    }

    /**
     * 일자 리스트를 콤마 구분 문자열로 변환
     * [1, 15, 30] → "1,15,30"
     */
    private String joinDaysOfMonth(List<Integer> daysOfMonth) {
        if (daysOfMonth == null || daysOfMonth.isEmpty()) {
            return null;
        }
        return daysOfMonth.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private record TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
