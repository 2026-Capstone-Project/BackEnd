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
    private static final double MIN_SCORE = 0.7; // 유사도 0.7 미만은 노이즈로 제거함

    public String build(Long memberId, String query) {
        float[] queryVector = embeddingClient.embed(query);
        List<QdrantSearchResult> results = qdrantVectorClient.search(memberId, queryVector, SEARCH_LIMIT);

        List<QdrantSearchResult> filtered = results.stream()
                .filter(r -> r.score() >= MIN_SCORE)
                .toList();

        if (filtered.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (QdrantSearchResult result : filtered) {
            String title = (String) result.payload().get("title");
            String startDate = (String) result.payload().get("startDate");
            String type = (String) result.payload().getOrDefault("type", "EVENT");
            String label = "TODO".equals(type) ? "[할 일]" : "[일정]";
            sb.append(String.format("- %s %s: %s%n", label, startDate, title));
        }
        return sb.toString().trim();
    }
}
