package com.project.backend.domain.chat.service;

import com.project.backend.domain.nlp.client.EmbeddingClient;
import com.project.backend.global.qdrant.client.QdrantVectorClient;
import com.project.backend.global.qdrant.dto.response.QdrantSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VectorContextBuilder {

    private final EmbeddingClient embeddingClient;
    private final QdrantVectorClient qdrantVectorClient;

    private static final int SEARCH_LIMIT = 5;
    private static final double MIN_SCORE = 0.50; // text-embedding-3-small 코사인 유사도 기준, 0.25는 이종 활동 명사 간 노이즈 허용 → 0.5로 상향

    public String build(Long memberId, String query) {
        float[] queryVector = embeddingClient.embed(query);
        List<QdrantSearchResult> results = qdrantVectorClient.search(memberId, queryVector, SEARCH_LIMIT);

        results.forEach(r -> log.debug("Qdrant 검색 결과 - id: {}, score: {}, payload: {}", r.id(), r.score(), r.payload()));

        List<QdrantSearchResult> filtered = results.stream()
                .filter(r -> r.score() >= MIN_SCORE)
                .toList();

        if (filtered.isEmpty()) {
            log.debug("Qdrant 유사도 임계값({}) 미달 또는 결과 없음", MIN_SCORE);
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (QdrantSearchResult result : filtered) {
            String title = (String) result.payload().get("title");
            String startDate = (String) result.payload().get("startDate");
            String type = (String) result.payload().getOrDefault("type", "EVENT");

            boolean isRecurring = Boolean.TRUE.equals(result.payload().get("isRecurring"));
            String label = "TODO".equals(type) ? "[할 일]" : "[일정]";
            String recurringMarker = isRecurring ? " (반복일정)" : "";

            // Qdrant point ID → DB ID 변환 (Todo는 오프셋 제거)
            long dbId = "TODO".equals(type) ? result.id() - 2_000_000_000L : result.id();

            sb.append(String.format("- %s [ID:%d, TYPE:%s]%s %s: %s%n",
                    label, dbId, type, recurringMarker, startDate, title));
        }
        return sb.toString().trim();
    }
}
