package com.project.backend.domain.nlp.client;

public interface EmbeddingClient {
    float[] embed(String text);          // 문서 색인용
    default float[] embedQuery(String text) { return embed(text); } // 검색 쿼리용 (기본은 embed와 동일)
}
