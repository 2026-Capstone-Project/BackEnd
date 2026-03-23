package com.project.backend.domain.event.service;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.nlp.client.EmbeddingClient;
import com.project.backend.global.qdrant.client.QdrantVectorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleVectorSyncService {

    private final EmbeddingClient embeddingClient;
    private final QdrantVectorClient qdrantVectorClient;

    public void syncOnCreate(Event event) {
        try {
            float[] vector = embeddingClient.embed(buildEmbedText(event));
            qdrantVectorClient.upsert(
                    event.getId(),
                    event.getMember().getId(),
                    event.getTitle(),
                    event.getStartTime().toLocalDate().toString(),
                    vector
            );
            log.debug("Qdrant 저장 완료 - eventId: {}", event.getId());
        } catch (Exception e) {
            // Qdrant 실패해도 일정 등록은 성공으로 처리
            log.error("Qdrant 동기화 실패 (create) - eventId: {}", event.getId(), e);
        }
    }

    public void syncOnUpdate(Event event) {
        // upsert = 있으면 업데이트, 없으면 새로 저장
        syncOnCreate(event);
    }

    public void syncOnDelete(Long eventId) {
        try {
            qdrantVectorClient.delete(eventId);
            log.debug("Qdrant 삭제 완료 - eventId: {}", eventId);
        } catch (Exception e) {
            log.error("Qdrant 동기화 실패 (delete) - eventId: {}", eventId, e);
        }
    }

    // 임베딩할 텍스트 : title + content + location 조합
    // 풍부할수록 의미 검색 정확도 향상
    private String buildEmbedText(Event event) {
        StringBuilder sb = new StringBuilder(event.getTitle());
        if (event.getContent() != null) {
            sb.append(" ").append(event.getContent());
        }
        if (event.getLocation() != null) {
            sb.append(" ").append(event.getLocation());
        }
        return sb.toString();
    }
}
