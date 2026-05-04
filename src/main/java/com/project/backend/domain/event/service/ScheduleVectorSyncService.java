package com.project.backend.domain.event.service;

import com.project.backend.domain.common.enums.VectorSyncStatus;
import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.nlp.client.EmbeddingClient;
import com.project.backend.global.qdrant.client.QdrantVectorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleVectorSyncService {

    private final EventRepository eventRepository;
    private final EmbeddingClient embeddingClient;
    private final QdrantVectorClient qdrantVectorClient;

    @Async("vectorSyncExecutor")
    @Transactional
    public void syncOnCreate(Long eventId) {
        Event event = eventRepository.findWithMemberById(eventId)
                .orElseThrow(() -> new IllegalStateException("벡터 동기화 대상 Event 없음 - eventId: " + eventId));
        doUpsertSync(event);
    }

    @Async("vectorSyncExecutor")
    @Transactional
    public void syncOnUpdate(Long eventId) {
        Event event = eventRepository.findWithMemberById(eventId)
                .orElseThrow(() -> new IllegalStateException("벡터 동기화 대상 Event 없음 - eventId: " + eventId));
        doUpsertSync(event);
    }

    @Async("vectorSyncExecutor")
    public void syncOnDelete(Long eventId) {
        try {
            qdrantVectorClient.delete(eventId);
            log.debug("Qdrant 삭제 완료 - eventId: {}", eventId);
        } catch (Exception e) {
            log.error("Qdrant 삭제 실패 - eventId: {}", eventId, e);
        }
    }

    private void doUpsertSync(Event event) {
        try {
            float[] vector = embeddingClient.embed(buildEmbedText(event));
            qdrantVectorClient.upsert(
                    event.getId(),
                    event.getMember().getId(),
                    event.getTitle(),
                    event.getStartTime().toLocalDate().toString(),
                    "EVENT",
                    event.getRecurrenceGroup() != null,
                    vector
            );
            event.updateVectorSyncStatus(VectorSyncStatus.SUCCESS);
            log.debug("Qdrant 동기화 완료 - eventId: {}", event.getId());
        } catch (Exception e) {
            log.error("Qdrant 동기화 실패 - eventId: {}", event.getId(), e);
            event.updateVectorSyncStatus(VectorSyncStatus.FAILED);
        }
    }

    private String buildEmbedText(Event event) {
        StringBuilder sb = new StringBuilder(event.getTitle());
        if (event.getContent() != null) sb.append(" ").append(event.getContent());
        if (event.getLocation() != null) sb.append(" ").append(event.getLocation());
        return sb.toString();
    }
}
