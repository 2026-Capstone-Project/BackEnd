package com.project.backend.domain.nlp.client;

public interface EmbeddingClient {
    float[] embed(String text);
}
