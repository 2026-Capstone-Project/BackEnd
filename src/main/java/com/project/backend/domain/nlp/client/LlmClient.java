package com.project.backend.domain.nlp.client;

import java.util.List;
import java.util.Map;

public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);

    String chatWithHistory(String systemPrompt, List<Map<String, String>> messages);
}
