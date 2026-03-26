package com.project.backend.global.qdrant.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QdrantSearchResult(
        Long id,
        double score, // 코사인 유사도(0~1)
        Map<String,Object> payload // { memberId, title, startDate }
) {
}
