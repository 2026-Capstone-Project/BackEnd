package com.project.backend.domain.nlp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.nlp.client.LlmClient;
import com.project.backend.domain.nlp.converter.NlpConverter;
import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.dto.response.LlmResDTO;
import com.project.backend.domain.nlp.dto.response.NlpResDTO;
import com.project.backend.domain.nlp.enums.ItemType;
import com.project.backend.domain.nlp.exception.NlpErrorCode;
import com.project.backend.domain.nlp.exception.NlpException;
import com.project.backend.domain.nlp.service.NlpService;
import com.project.backend.domain.nlp.service.PromptTemplate;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlpServiceImpl implements NlpService {

    private final ObjectMapper objectMapper;
    private final LlmClient llmClient;
    private final PromptTemplate promptTemplate;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final TodoRepository todoRepository;

    @Override
    public NlpResDTO.ParseRes parse(NlpReqDTO.ParseReq reqDTO, Long memberId) {
        LocalDate baseDate = reqDTO.baseDate() != null ? reqDTO.baseDate() : LocalDate.now();

        String systemPrompt = promptTemplate.getSystemPrompt(baseDate); // AI에게 주는 메뉴얼
        String userPrompt = promptTemplate.getUserPrompt(reqDTO.text()); // 유저의 실제 요청
        log.info("LLM 파싱 요청 - memberId: {}, text: {}", memberId, reqDTO.text());

        String llmResponse = llmClient.chat(systemPrompt, userPrompt);
        return parseLlmResponse(llmResponse);
    }

    @Override
    @Transactional
    public NlpResDTO.ConfirmRes confirm(NlpReqDTO.ConfirmReq reqDTO, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        List<NlpResDTO.ConfirmResult> results = new ArrayList<>();
        int successCount = 0; int failCount = 0;

        for (NlpReqDTO.ConfirmItem item : reqDTO.items()) {
            try {
                List<Long> savedIds = saveItem(item, member);
                results.add(NlpResDTO.ConfirmResult.success(savedIds, item.type(), item.title()));
                successCount++;
                log.info("저장 성공 - type: {}, title: {}, count: {}", item.type(), item.title(), savedIds.size());
            } catch (Exception e) {
                results.add(NlpResDTO.ConfirmResult.failure(item.type(), item.title(), e.getMessage()));
                failCount++;
                log.error("저장 실패 - type: {}, title: {}", item.type(), item.title(), e);
            }
        }
        return NlpResDTO.ConfirmRes.builder()
                .totalCount(reqDTO.items().size())
                .successCount(successCount)
                .failCount(failCount)
                .results(results)
                .message(buildResultMessage(results))
                .build();
    }

    private NlpResDTO.ParseRes parseLlmResponse(String llmResponse) {
        try {
            String jsonStr = extractJson(llmResponse);
            LlmResDTO llmResDTO = objectMapper.readValue(jsonStr, LlmResDTO.class);

            if (llmResDTO.isSingleItem()) {
                // 단일 항목 -> LlmResDTO 자체를 반환함
                NlpResDTO.ParsedItem item = NlpConverter.toParsedItem(llmResDTO);
                return NlpResDTO.ParseRes.single(item);
            } else {
                // 복수 항목 -> items 배열의 각 LlmParsedItem을 반환함
                List<NlpResDTO.ParsedItem> items = llmResDTO.items().stream()
                        .map(NlpConverter::toParsedItem)
                        .toList();
                return NlpResDTO.ParseRes.multiple(items);
            }
        } catch (JsonProcessingException e) {
            log.error("LLM 응답 파싱 실패: {}", llmResponse, e);
            throw new NlpException(NlpErrorCode.LLM_PARSE_ERROR);
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}") + 1;

        if (start == -1 || end == 0) {
            throw new NlpException(NlpErrorCode.LLM_PARSE_ERROR);
        }

        return trimmed.substring(start, end);
    }

    private List<Long> saveItem(NlpReqDTO.ConfirmItem item, Member member) {
        return switch(item.type()) {
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
        LocalDateTime startTime = LocalDateTime.of(item.date(), item.time() != null ? item.time() : LocalTime.of(0,0));
        LocalDateTime endTime = startTime.plusHours(1);

        Event event = Event.createFromNaturalLanguage(member, item.title(), startTime, endTime, null);
        eventRepository.save(event);
        return event.getId();
    }

    private List<Long> saveRecurringEvents(NlpReqDTO.ConfirmItem item, Member member) {
        NlpReqDTO.RecurrenceRule rule = item.recurrenceRule();
        List<LocalDate> dates = calculateRecurringDates(item.date(), rule);

        String groupId = UUID.randomUUID().toString();
        List<Long> savedIds = new ArrayList<>();

        for (LocalDate date : dates) {
            LocalDateTime startTime = LocalDateTime.of(date, item.time() != null ? item.time() : LocalTime.of(0, 0));
            LocalDateTime endTime = startTime.plusHours(1);

            Event event = Event.createFromNaturalLanguage(member, item.title(), startTime, endTime, groupId);

            eventRepository.save(event);
            savedIds.add(event.getId());
        }

        log.info("반복 일정 생성 완료 - groupId: {}, count: {}", groupId, savedIds.size());
        return savedIds;
    }

    private List<Long> saveTodo(NlpReqDTO.ConfirmItem item, Member member) {
        LocalDateTime dueTime = item.date() != null
                ? LocalDateTime.of(item.date(), item.time() != null ? item.time() : LocalTime.of(23, 59))
                : null;

        Todo todo = Todo.builder()
                .member(member)
                .title(item.title())
                .dueTime(dueTime)
                .isCompleted(false)
                .build();

        todoRepository.save(todo);
        return List.of(todo.getId());
    }

    private List<LocalDate> calculateRecurringDates(LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        List<LocalDate> dates = new ArrayList<>();

        LocalDate endDate = rule.endDate() != null
                ? rule.endDate()
                : startDate.plusMonths(3);

        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (matchesRule(current, startDate, rule)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }

        return dates;
    }

    private boolean matchesRule(LocalDate date, LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        return switch (rule.frequency()) {
            case DAILY -> matchesDailyRule(date, startDate, rule);
            case WEEKLY -> matchesWeeklyRule(date, startDate, rule);
            case MONTHLY -> matchesMonthlyRule(date, startDate, rule);
            case YEARLY -> matchesYearlyRule(date, startDate, rule);
        };
    }

    private boolean matchesDailyRule(LocalDate date, LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, date);
        return daysBetween % rule.getIntervalOrDefault() == 0;
    }

    private boolean matchesWeeklyRule(LocalDate date, LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        if (rule.daysOfWeek() != null && !rule.daysOfWeek().isEmpty()) {
            String dayName = date.getDayOfWeek().name().substring(0, 3);
            if (!rule.daysOfWeek().contains(dayName)) {
                return false;
            }
        }

        if (rule.getIntervalOrDefault() > 1) {
            long weeksBetween = ChronoUnit.WEEKS.between(
                    startDate.with(DayOfWeek.MONDAY),
                    date.with(DayOfWeek.MONDAY)
            );
            return weeksBetween % rule.getIntervalOrDefault() == 0;
        }

        return true;
    }

    private boolean matchesMonthlyRule(LocalDate date, LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        if (date.getDayOfMonth() != startDate.getDayOfMonth()) {
            return false;
        }
        long monthsBetween = ChronoUnit.MONTHS.between(startDate, date);
        return monthsBetween % rule.getIntervalOrDefault() == 0;
    }

    private boolean matchesYearlyRule(LocalDate date, LocalDate startDate, NlpReqDTO.RecurrenceRule rule) {
        if (date.getMonth() != startDate.getMonth()) {
            return false;
        }
        if (date.getDayOfMonth() != startDate.getDayOfMonth()) {
            return false;
        }
        long yearsBetween = ChronoUnit.YEARS.between(startDate, date);
        return yearsBetween % rule.getIntervalOrDefault() == 0;
    }

    private String buildResultMessage(List<NlpResDTO.ConfirmResult> results) {
        int totalCreated = results.stream()
                .filter(NlpResDTO.ConfirmResult::success)
                .mapToInt(NlpResDTO.ConfirmResult::count)
                .sum();

        long failCount = results.stream()
                .filter(r -> !r.success())
                .count();

        if (failCount == 0) {
            if (totalCreated == 1) {
                ItemType type = results.get(0).type();
                return type == ItemType.EVENT
                        ? "일정이 등록되었어요!"
                        : "할 일이 등록되었어요!";
            } else {
                return totalCreated + "개 일정이 등록되었어요!";
            }
        } else {
            return totalCreated + "개 등록, " + failCount + "개 실패했어요.";
        }
    }
}
