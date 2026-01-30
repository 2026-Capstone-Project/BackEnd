package com.project.backend.domain.event.strategy.generator;

import com.project.backend.domain.event.entity.RecurrenceGroup;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DefaultGenerator implements Generator {

    // 반복이 없는 경우에는 null 반환
    @Override
    public LocalDateTime next(LocalDateTime current, RecurrenceGroup rg) {
        return null;
    }
}
