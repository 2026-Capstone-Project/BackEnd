package com.project.backend.domain.chat.service;

import java.util.List;
import java.util.Map;

public interface ConversationHistoryService {

    List<Map<String, String>> getHistory(Long memberId);

    void saveMessage(Long memberId, String role, String content);

    void clearHistory(Long memberId);

    void savePendingContext(Long memberId, Long scheduleId, String scheduleType);

    Map<String, String> getPendingContext(Long memberId);

    void clearPendingContext(Long memberId);

    void saveLastActionContext(Long memberId, Long scheduleId, String scheduleType);

    Map<String, String> getLastActionContext(Long memberId);
}
