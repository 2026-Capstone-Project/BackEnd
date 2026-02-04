package com.project.backend.domain.reminder.factory;

import com.project.backend.domain.reminder.enums.TargetType;
import org.springframework.stereotype.Component;

@Component
public class ReminderMessageFactory {

    public String create(String title, int minutes, TargetType type) {
        if (type == TargetType.EVENT) {
            return eventMessage(title, minutes);
        }
        return todoMessage(title, minutes);
    }

    private String eventMessage(String title, int minutes) {
        if (minutes < 60) {
            return "%d분 뒤, %s 일정이에요.".formatted(minutes, title);
        }
        return "%d시간 뒤, %s 일정이에요.".formatted(minutes / 60, title);
    }

    private String todoMessage(String title, int minutes) {
        if (minutes < 60) {
            return "%d분 뒤, %s 마감이에요.".formatted(minutes, title);
        }
        return "%d시간 뒤, %s 마감이에요.".formatted(minutes / 60, title);
    }
}