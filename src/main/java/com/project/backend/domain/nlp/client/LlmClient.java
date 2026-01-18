package com.project.backend.domain.nlp.client;

public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);
}
