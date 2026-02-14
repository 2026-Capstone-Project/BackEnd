package com.project.backend.domain.reminder.service.query;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.entity.RecurrenceException;
import com.project.backend.domain.event.exception.EventErrorCode;
import com.project.backend.domain.event.exception.EventException;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.repository.RecurrenceExceptionRepository;
import com.project.backend.domain.reminder.converter.ReminderConverter;
import com.project.backend.domain.reminder.enums.LifecycleStatus;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.reminder.factory.ReminderMessageFactory;
import com.project.backend.domain.reminder.repository.ReminderRepository;
import com.project.backend.domain.reminder.dto.response.ReminderResDTO;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.setting.entity.Setting;
import com.project.backend.domain.setting.exception.SettingErrorCode;
import com.project.backend.domain.setting.exception.SettingException;
import com.project.backend.domain.setting.repository.SettingRepository;
import com.project.backend.domain.todo.repository.TodoRecurrenceExceptionRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderQueryServiceImpl implements ReminderQueryService{

    private final ReminderRepository reminderRepository;
    private final SettingRepository settingRepository;
    private final ReminderMessageFactory reminderMessageFactory;
    private final RecurrenceExceptionRepository recurrenceExceptionRepository;
    private final EventRepository eventRepository;
    private final TodoRepository todoRepository;
    private final TodoRecurrenceExceptionRepository todoRecurrenceExceptionRepository;

    @Override
    public List<ReminderResDTO.DetailRes> getReminder(Long memberId) {
        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        // 현재시간보다 저장된 리마인더의 occurrenceTime이 더 이후에 있는 리마인더만 반환 (이미 지나서 필요없는 리마인더 제외)
        List<Reminder> reminders = reminderRepository.
                findVisibleReminders(
                        memberId,
                        LifecycleStatus.ACTIVE,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusMinutes(setting.getReminderTiming().getMinutes())
                );

        return reminders.stream()
                .map(reminder ->
                        ReminderConverter.toDetailRes(
                                reminder,
                                setting.getReminderTiming().getMinutes(),
                                resolveReminderTitle(reminder),
                                createMessage(reminder, setting)
                        )
                )
                .toList();
    }

    private String createMessage(Reminder reminder, Setting setting) {
        return reminderMessageFactory.create(
                reminder.getTitle(),
                setting.getReminderTiming().getMinutes(),
                reminder.getTargetType()
        );
    }

    private String resolveReminderTitle(Reminder reminder) {
        Optional<RecurrenceException> re = Optional.empty();

        if (reminder.getTargetType() == TargetType.EVENT) {
            Event event = eventRepository.findById(reminder.getTargetId())
                    .orElseThrow(() -> new EventException(EventErrorCode.EVENT_NOT_FOUND));

            // 단일 일정이면 RecurrenceException은 존재하지 않는다.
            if (!event.isRecurring()) {
                return reminder.getTitle();
            }

            re = recurrenceExceptionRepository.findByRecurrenceGroupIdAndExceptionDate(
                    event.getRecurrenceGroup().getId(),
                    reminder.getOccurrenceTime().toLocalDate()
            );
        } else {
            // Todo 반복그룹 통합 필요
//            Todo todo = todoRepository.findById(reminder.getTargetId())
//                    .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));
//            re = todoRecurrenceExceptionRepository.findByTodoRecurrenceGroupIdAndExceptionDate(
//                    todo.getTodoRecurrenceGroup().getId(),
//                    reminder.getOccurrenceTime().toLocalDate()
//            )
        }

        // 예외에 제목이 있다면 제목 변경된 것이므로 조회시 해당 제목 사용
        if (re.isEmpty()) {
            return reminder.getTitle();
        }

        return re.get().getTitle();
    }
}
