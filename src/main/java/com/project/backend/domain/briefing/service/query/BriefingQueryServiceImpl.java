package com.project.backend.domain.briefing.service.query;

import com.project.backend.domain.briefing.converter.BriefingConverter;
import com.project.backend.domain.occurrence.dto.TodayOccurrenceResult;
import com.project.backend.domain.briefing.enums.BriefingReason;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.briefing.dto.response.BriefingResDTO;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.occurrence.service.OccurrenceResolver;
import com.project.backend.domain.setting.entity.Setting;
import com.project.backend.domain.setting.exception.SettingErrorCode;
import com.project.backend.domain.setting.exception.SettingException;
import com.project.backend.domain.setting.repository.SettingRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BriefingQueryServiceImpl implements BriefingQueryService {

    private final SettingRepository settingRepository;
    private final EventRepository eventRepository;
    private final TodoRepository todoRepository;
    private final OccurrenceResolver occurrenceResolver;

    public BriefingResDTO.BriefingRes getBriefing(Long memberId) {
        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 브리핑 비활성화 시
        if (!setting.getDailyBriefing()) {
            return BriefingConverter.toBriefingRes(today, BriefingReason.DISABLED);
        }

        // 설정한 브리핑 시간이 현재시간보다 이후이면 리턴
        if (now.isBefore(setting.getDailyBriefingTime().atDate(today))) {
            return BriefingConverter.toBriefingRes(today, BriefingReason.TIME_NOT_REACHED);
        }

        // 현재시간보다 startTime이 이후가 아닌 일정/투두 조회
        List<Long> eventIds = eventRepository.findEventIdsByMemberIdAndCurrentDate(
                        memberId, today.atTime(LocalTime.MAX));

        List<Long> todoIds = todoRepository.findTodoIdsByMemberIdAndCurrentDate(memberId, today);

        List<BriefingResDTO.BriefInfoRes> eventBrief =
                eventIds.isEmpty() ? List.of() : toBriefInfos(TargetType.EVENT, eventIds, today);
        List<BriefingResDTO.BriefInfoRes> todoBrief =
                todoIds.isEmpty() ? List.of() : toBriefInfos(TargetType.TODO,  todoIds, today);

        // 일정과 할일이 없다면 빈 값 리턴
        if (eventBrief.isEmpty() && todoBrief.isEmpty()) {
            return BriefingConverter.toBriefingRes(today, BriefingReason.NOT_EVENT_TODAY);
        }

        List<BriefingResDTO.BriefInfoRes> briefInfo =
                Stream.concat(eventBrief.stream(), todoBrief.stream())
                        .sorted(Comparator.comparing(BriefingResDTO.BriefInfoRes::startTime))
                        .toList();

        return BriefingConverter.toBriefingRes(
                today,
                BriefingReason.AVAILABLE,
                briefInfo,
                eventBrief.size(),
                todoBrief.size()
        );
    }

    private List<BriefingResDTO.BriefInfoRes> toBriefInfos(TargetType type, List<Long> ids, LocalDate date) {
        if (ids.isEmpty()) return List.of();

        return occurrenceResolver.getTodayOccurrence(type, ids, date).stream()
                .filter(TodayOccurrenceResult::hasToday)
                .map(BriefingConverter::toBriefInfoRes)
                .toList();
    }
}
