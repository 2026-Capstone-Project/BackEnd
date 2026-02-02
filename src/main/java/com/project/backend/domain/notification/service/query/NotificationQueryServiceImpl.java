package com.project.backend.domain.notification.service.query;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.notification.converter.NotificationConverter;
import com.project.backend.domain.notification.dto.response.NotificationResDTO;
import com.project.backend.domain.notification.enums.BriefingReason;
import com.project.backend.domain.setting.entity.Setting;
import com.project.backend.domain.setting.exception.SettingErrorCode;
import com.project.backend.domain.setting.exception.SettingException;
import com.project.backend.domain.setting.repository.SettingRepository;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final SettingRepository settingRepository;
    private final EventRepository eventRepository;
    private final TodoRepository todoRepository;

    public NotificationResDTO.BriefingRes getBriefing(Long memberId) {
        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        if (!setting.getDailyBriefing()) {
            return NotificationResDTO.BriefingRes.unavailable(now.toLocalDate());
        }

        if (now.isBefore(setting.getDailyBriefingTime().atDate(now.toLocalDate()))) {
            return NotificationResDTO.BriefingRes.unavailable(now.toLocalDate());
        }

        List<Event> events = eventRepository.findAllByMemberId(memberId);
        List<Todo> todos = todoRepository.findAllByMemberId(memberId);

        if (events.isEmpty() && todos.isEmpty()) {
            return NotificationResDTO.BriefingRes.empty(now.toLocalDate());
        }

//        List<NotificationResDTO.BriefInfoRes> allInfo =
//                Stream.concat(events.stream().map(NotificationConverter::toBriefEventInfoRes).toList(),
//                                todos.stream().map(NotificationConverter::toBriefTodoInfoRes).toList())
//                        .sorted(Comparator.comparing(NotificationResDTO.BriefInfoRes::startTime))
//                        .toList();
//
//        return NotificationConverter.toBriefingRes(
//                now.toLocalDate(),
//                BriefingReason.AVAILABLE,
//                allInfo,
//                events.size(),
//                todos.size()
//        );
        return null;
    }
}
