package com.project.backend.global.scheduler;

import com.project.backend.domain.common.enums.VectorSyncStatus;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.event.service.ScheduleVectorSyncService;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.TodoRepository;
import com.project.backend.domain.todo.service.command.TodoVectorSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorSyncRetryScheduler {

    private static final int PENDING_TIMEOUT_MINUTES = 5;

    private final TodoRepository todoRepository;
    private final EventRepository eventRepository;
    private final TodoVectorSyncService todoVectorSyncService;
    private final ScheduleVectorSyncService scheduleVectorSyncService;

    @Scheduled(fixedDelay = 300_000)
    public void retryFailedSync() {
        LocalDateTime cutOff = LocalDateTime.now().minusMinutes(PENDING_TIMEOUT_MINUTES);

        List<Todo> failedTodos = todoRepository.findSyncRetryTargets(VectorSyncStatus.FAILED, VectorSyncStatus.PENDING, cutOff);
        List<Event> failedEvents = eventRepository.findSyncRetryTargets(VectorSyncStatus.FAILED, VectorSyncStatus.PENDING, cutOff);

        if (failedTodos.isEmpty() && failedEvents.isEmpty()) {
            return;
        }

        log.info("[VectorSyncRetry] 재시도 시작 - todos={}, events={}",
                failedTodos.size(), failedEvents.size());

        failedTodos.forEach(todo -> todoVectorSyncService.syncOnCreate(todo.getId()));
        failedEvents.forEach(event -> scheduleVectorSyncService.syncOnCreate(event.getId()));

        log.info("[VectorSyncRetry] 재시도 제출 완료");
    }

}
