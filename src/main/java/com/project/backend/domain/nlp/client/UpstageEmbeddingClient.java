package com.project.backend.domain.nlp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.llm.embedding-provider", havingValue = "upstage")
public class UpstageEmbeddingClient implements EmbeddingClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.llm.embedding-api-key}")
    private String apiKey;

    private static final String PASSAGE_MODEL = "solar-embedding-1-large-passage";
    private static final String QUERY_MODEL = "solar-embedding-1-large-query";
    private static final String EMBEDDING_URL = "https://api.upstage.ai/v1/embeddings";

    @Override
    public float[] embed(String text) {
        return callEmbeddingApi(PASSAGE_MODEL, text);
    }

    @Override
    public float[] embedQuery(String text) {
        return callEmbeddingApi(QUERY_MODEL, text);
    }

    private float[] callEmbeddingApi(String model, String text) {
        try {
            String response = webClient.post()
                    .uri(EMBEDDING_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of("model", model, "input", text))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode vectorNode = root.path("data").get(0).path("embedding");

            float[] vector = new float[vectorNode.size()];
            for (int i = 0; i < vectorNode.size(); i++) {
                vector[i] = (float) vectorNode.get(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            log.error("임베딩 생성 실패 - model: {}, error: {}", model, e.getMessage());
            throw new RuntimeException("임베딩 생성 실패", e);
        }
    }
}
