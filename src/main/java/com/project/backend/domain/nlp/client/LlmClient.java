package com.project.backend.domain.nlp.client;

import java.util.List;
import java.util.Map;

public interface LlmClient {
    String chat(String systemPrompt, String userPrompt);

    String chatWithHistory(String systemPrompt, List<Map<String, String>> messages);

    // tool_calls 메시지는 "tool_calls" 키에 List 형태의 중첩 구조가 필요 -> messages를 Map<String, Object>로 받는 이유
    FunctionCallResponse chatWithFunctions(String systemPrompt,
                                           List<Map<String, Object>> messages,
                                           List<Map<String, Object>> tools);
}
