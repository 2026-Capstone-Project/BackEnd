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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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

        LocalDateTime today = LocalDateTime.now();

        // 브리핑 비활성화 시
        if (!setting.getDailyBriefing()) {
            return BriefingConverter.toDisable(today.toLocalDate());
        }

        // 설정한 브리핑 시간이 현재시간보다 이후이면 리턴
        if (today.isBefore(setting.getDailyBriefingTime().atDate(today.toLocalDate()))) {
            return BriefingConverter.toTimeNotReached(today.toLocalDate());
        }

        // 현재시간보다 startTime이 이후가 아닌 일정/투두 조회
        List<Long> eventIds = eventRepository
                .findAllByMemberIdAndCurrentDate(
                        memberId, today.toLocalDate().atTime(23, 59, 59)
                )
                .stream()
                .map(Event::getId)
                .toList();

        List<Long> todoIds = todoRepository
                .findAllByMemberIdAndCurrentDate(memberId, today.toLocalDate())
                .stream()
                .map(Todo::getId)
                .toList();

        List<BriefingResDTO.BriefInfoRes> eventBrief = List.of();
        List<BriefingResDTO.BriefInfoRes> todoBrief = List.of();

        if (!eventIds.isEmpty()) {
            eventBrief = toBriefInfos(TargetType.EVENT, eventIds, today.toLocalDate());
        }

        if (!todoIds.isEmpty()) {
            todoBrief  = toBriefInfos(TargetType.TODO,  todoIds,  today.toLocalDate());
        }

        // 일정과 할일이 없다면 빈 값 리턴
        if (eventBrief.isEmpty() && todoBrief.isEmpty()) {
            return BriefingConverter.toEmpty(today.toLocalDate());
        }

        List<BriefingResDTO.BriefInfoRes> briefInfo =
                Stream.concat(eventBrief.stream(), todoBrief.stream())
                        .sorted(Comparator.comparing(BriefingResDTO.BriefInfoRes::startTime))
                        .toList();

        return BriefingConverter.toBriefingRes(
                today.toLocalDate(),
                BriefingReason.AVAILABLE,
                briefInfo,
                eventBrief.size(),
                todoBrief.size()
        );
    }

    private List<BriefingResDTO.BriefInfoRes> toBriefInfos(TargetType type, List<Long> ids, LocalDate date) {
        if (ids.isEmpty()) return List.of();

        return occurrenceProvider.getTodayOccurrence(type, ids, date).stream()
                .filter(TodayOccurrenceResult::hasToday)
                .map(BriefingConverter::toBriefInfoRes)
                .toList();
    }
}
