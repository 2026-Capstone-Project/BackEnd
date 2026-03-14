package com.project.backend.domain.chat.service;

import java.util.List;
import java.util.Map;

public interface ConversationHistoryService {

    List<Map<String, String>> getHistory(Long memberId);

    void saveMessage(Long memberId, String role, String content);

    void clearHistory(Long memberId);
}
