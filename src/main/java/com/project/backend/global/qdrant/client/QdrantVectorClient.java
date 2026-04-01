package com.project.backend.global.qdrant.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.global.qdrant.dto.response.QdrantSearchResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantVectorClient {

    private final ObjectMapper objectMapper;
    private WebClient webClient;

    @Value("${spring.qdrant.host}")
    private String host;

    @Value("${spring.qdrant.port}")
    private String port;

    @Value("${spring.qdrant.collection}")
    private String collection;

    @Value("${spring.qdrant.vector-size}")
    private int vectorSize;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("http://" + host + ":" + port) // 내부 통신이어서 http
                .build();
        ensureCollectionExists();
    }

    // 컬렉션 없으면 자동 생성
    private void ensureCollectionExists() {
        try {
            webClient.put()
                    .uri("/collections/" + collection)
                    .bodyValue(Map.of("vectors", Map.of(
                            "size", vectorSize,
                            "distance", "Cosine")))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Qdrant 컬렉션 준비 완료 : {}", collection);
        } catch (Exception e) {
            log.warn("Qdrant 컬렉션 생성 실패 (이미 존재할 수 있음) : {}", e.getMessage());
        }
    }

    // 벡터 저장 및 업데이트
    public void upsert(Long pointId, Long memberId, String title, String startDate, String type, boolean isRecurring, float[] vector) {
        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("memberId", memberId);
            payload.put("title", title);
            payload.put("startDate", startDate);
            payload.put("type", type);
            payload.put("isRecurring", isRecurring);

            Map<String, Object> point = Map.of(
                    "id", pointId,
                    "vector", toList(vector),
                    "payload", payload
            );

            webClient.put()
                    .uri("/collections/" + collection + "/points")
                    .bodyValue(Map.of("points", List.of(point)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Qdrant upsert 실패 - pointId: {}, error: {}", pointId, e.getMessage());
            throw new RuntimeException("Qdrant upsert 실패", e);
        }
    }

    // 유사도 검색 (memberId 필터링으로 다른 사용자 결과 차단)
    public List<QdrantSearchResult> search(Long memberId, float[] queryVector, int limit) {
        try {
            Map<String, Object> body = Map.of(
                    "vector", toList(queryVector),
                    "filter", Map.of("must", List.of(
                                    Map.of("key", "memberId",
                                            "match", Map.of("value", memberId))
                            )
                    ),
                    "limit", limit,
                    "with_payload", true
            );

            String response = webClient.post()
                    .uri("/collections/" + collection + "/points/search")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode results = objectMapper.readTree(response).path("result");
            return objectMapper.convertValue(results,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QdrantSearchResult.class));
        } catch (Exception e) {
            log.error("Qdrant 검색 실패 - memberId: {}, error: {}", memberId, e.getMessage());
            throw new RuntimeException("Qdrant 검색 실패", e);
        }
    }

    // 벡터 삭제
    public void delete(Long eventId) {
        try {
            webClient.post()
                    .uri("/collections/" + collection + "/points/delete")
                    .bodyValue(Map.of("points", List.of(eventId)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Qdrant 삭제 실패 - eventId: {}, error: {}", eventId, e.getMessage());
            throw new RuntimeException("Qdrant 삭제 실패", e);
        }
    }

    private List<Float> toList(float[] vector) {
        List<Float> list = new ArrayList<>();
        for (float v : vector) list.add(v);
        return list;
    }
}
