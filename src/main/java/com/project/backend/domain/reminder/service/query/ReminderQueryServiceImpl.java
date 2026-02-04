package com.project.backend.domain.reminder.service.query;

import com.project.backend.domain.reminder.converter.ReminderConverter;
import com.project.backend.domain.reminder.factory.ReminderMessageFactory;
import com.project.backend.domain.reminder.repository.ReminderRepository;
import com.project.backend.domain.reminder.dto.response.ReminderResDTO;
import com.project.backend.domain.reminder.entity.Reminder;
import com.project.backend.domain.setting.entity.Setting;
import com.project.backend.domain.setting.exception.SettingErrorCode;
import com.project.backend.domain.setting.exception.SettingException;
import com.project.backend.domain.setting.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderQueryServiceImpl implements ReminderQueryService{

    private final ReminderRepository reminderRepository;
    private final SettingRepository settingRepository;
    private final ReminderMessageFactory reminderMessageFactory;

    @Override
    public List<ReminderResDTO.DetailRes> getReminder(Long memberId) {
        Setting setting = settingRepository.findByMemberId(memberId)
                .orElseThrow(() -> new SettingException(SettingErrorCode.SETTING_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        // 현재시간보다 저장된 리마인더의 occurrenceTime이 더 이후에 있는 리마인더만 반환 (이미 지나서 필요없는 리마인더 제외)
        List<Reminder> reminders = reminderRepository.findAllByMemberIdAndCurrentTime(memberId, now);

        return reminders.stream()
                .map(reminder ->
                        ReminderConverter.toDetailRes(
                                reminder,
                                setting.getReminderTiming().getMinutes(),
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
}
