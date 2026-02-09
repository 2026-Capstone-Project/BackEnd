package com.project.backend.domain.briefing.service.query;

import com.project.backend.domain.briefing.converter.BriefingConverter;
import com.project.backend.domain.briefing.dto.TodayOccurrenceResult;
import com.project.backend.domain.briefing.enums.BriefingReason;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.briefing.dto.response.BriefingResDTO;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.reminder.provider.OccurrenceProvider;
import com.project.backend.domain.setting.entity.Setting;
import com.project.backend.domain.setting.exception.SettingErrorCode;
import com.project.backend.domain.setting.exception.SettingException;
import com.project.backend.domain.setting.repository.SettingRepository;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BriefingQueryServiceImpl implements BriefingQueryService {

    private final SettingRepository settingRepository;
    private final EventRepository eventRepository;
    private final TodoRepository todoRepository;
    private final OccurrenceProvider occurrenceProvider;

    public BriefingResDTO.BriefingRes getBriefing(Long memberId) {
        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // 브리핑 비활성화 시 or 설정한 브리핑 시간이 현재시간보다 이후이면 리턴
        if (!setting.getDailyBriefing() || now.isBefore(setting.getDailyBriefingTime().atDate(now.toLocalDate()))) {
            return BriefingConverter.toUnavailable(now.toLocalDate());
        }

        // 현재시간보다 startTime이 이후가 아닌 일정/투두 조회
        List<Event> events = eventRepository.findAllByMemberIdAndCurrentDate(
                memberId,
                now.toLocalDate().atTime(LocalTime.of(23,59, 59))
                );
        List<Todo> todos = todoRepository.findAllByMemberIdAndCurrentDate(
                memberId,
                now.toLocalDate()
        );

        // 일정과 할일이 없다면 빈 값 리턴
        if (events.isEmpty() && todos.isEmpty()) {
            return BriefingConverter.toEmpty(now.toLocalDate());
        }

        // 브리핑 대상 일정/할일 수집
        List<BriefingResDTO.BriefInfoRes> eventBrief = occurrenceProvider.getTodayOccurrence(
                TargetType.EVENT,
                events.stream().map(Event::getId).toList(),
                now.toLocalDate()
        ).stream()
                .filter(TodayOccurrenceResult::hasToday)
                .map(BriefingConverter::toBriefInfoRes)
                .toList();

        List<BriefingResDTO.BriefInfoRes> todoBrief = occurrenceProvider.getTodayOccurrence(
                TargetType.TODO,
                todos.stream().map(Todo::getId).toList(),
                now.toLocalDate()
        ).stream()
                .filter(TodayOccurrenceResult::hasToday)
                .map(BriefingConverter::toBriefInfoRes)
                .toList();

        List<BriefingResDTO.BriefInfoRes> briefInfo = new ArrayList<>();

        briefInfo.addAll(eventBrief);
        briefInfo.addAll(todoBrief);

        return BriefingConverter.toBriefingRes(
                now.toLocalDate(),
                BriefingReason.AVAILABLE,
                briefInfo,
                eventBrief.size(),
                todoBrief.size()
        );
    }
}
