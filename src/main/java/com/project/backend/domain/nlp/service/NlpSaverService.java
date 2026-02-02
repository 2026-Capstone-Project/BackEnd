package com.project.backend.domain.nlp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlpSaverService {

    private final EventRepository eventRepository;
    private final RecurrenceGroupRepository recurrenceGroupRepository;
    private final TodoRepository todoRepository;
    private final RecurrenceCalculator recurrenceCalculator;
    private final ObjectMapper objectMapper;

    public List<Long> save(NlpReqDTO.ConfirmItem item, Member member) {
        return switch (item.type()) {
            case EVENT -> saveEvent(item, member);
            case TODO -> saveTodo(item, member);
            default -> throw new NlpException(NlpErrorCode.INVALID_ITEM_TYPE);
        };
    }

    private List<Long> saveEvent(NlpReqDTO.ConfirmItem item, Member member) {
        if (item.isRecurring() && item.recurrenceRule() != null) {
            return saveRecurringEvents(item, member);
        } else {
            return List.of(saveSingleEvent(item, member));
        }
    }

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

    private List<Long> saveRecurringEvents(NlpReqDTO.ConfirmItem item, Member member) {
        NlpReqDTO.RecurrenceRule rule = item.recurrenceRule();

        // 1. 반복 날짜 계산
        List<LocalDate> dates = recurrenceCalculator.calculate(item.date(), rule);

        // 2. RecurrenceGroup 생성
        RecurrenceGroup group = createRecurrenceGroup(member, rule, dates.size());
        recurrenceGroupRepository.save(group);

        // 3. 개별 Event 생성
        RecurrenceFrequency recurrenceFrequency = rule.frequency();
        List<Long> savedIds = new ArrayList<>();

        for (LocalDate date : dates) {
            TimeRange timeRange = calculateTimeRange(item, date);

            Event event = Event.createRecurring(
                    member,
                    item.title(),
                    timeRange.startTime(),
                    timeRange.endTime(),
                    item.durationMinutes(),
                    item.isAllDay(),
                    recurrenceFrequency,
                    group,
                    item.getColorOrDefault()
            );

            eventRepository.save(event);
            savedIds.add(event.getId());
        }

        log.debug("반복 일정 생성 완료 - groupId: {}, count: {}", group.getId(), savedIds.size());
        return savedIds;
    }

    private RecurrenceGroup createRecurrenceGroup(Member member, NlpReqDTO.RecurrenceRule rule, int createdCount) {
        return RecurrenceGroup.create(
                member,
                rule.frequency(),
                rule.getIntervalOrDefault(),
                serializeDaysOfWeek(rule.daysOfWeek()),
                rule.monthlyType(),
                serializeDaysOfMonth(rule.daysOfMonth()),
                rule.weekOfMonth(),
                rule.dayOfWeekInMonth(),
                rule.monthOfYear(),
                mapToEndType(rule.endType()),
                rule.endDate(),
                rule.occurrenceCount(),
                createdCount
        );
    }

    private String serializeDaysOfWeek(List<String> daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(daysOfWeek);
        } catch (JsonProcessingException e) {
            log.error("daysOfWeek 직렬화 실패", e);
            return null;
        }
    }

    private String serializeDaysOfMonth(List<Integer> daysOfMonth) {
        if (daysOfMonth == null || daysOfMonth.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(daysOfMonth);
        } catch (JsonProcessingException e) {
            log.error("daysOfMonth 직렬화 실패", e);
            return null;
        }
    }

    private List<Long> saveTodo(NlpReqDTO.ConfirmItem item, Member member) {
        LocalDate startDate = item.date();
        LocalTime dueTime = item.getStartTimeOrDefault();

        Todo todo = Todo.builder()
                .member(member)
                .title(item.title())
                .startDate(startDate)
                .dueTime(dueTime)
                .isCompleted(false)
                .build();

        todoRepository.save(todo);
        return List.of(todo.getId());
    }

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
            end = null;
        }

        return new TimeRange(start, end);
    }


    private RecurrenceEndType mapToEndType(RecurrenceEndType endType) {
        if (endType == null) {
            return RecurrenceEndType.NEVER;
        }
        return switch (endType) {
            case NEVER -> RecurrenceEndType.NEVER;
            case END_BY_DATE -> RecurrenceEndType.END_BY_DATE;
            case END_BY_COUNT -> RecurrenceEndType.END_BY_COUNT;
        };
    }

    private record TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
    }
}
