package com.project.backend.domain.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationHistoryServiceImpl implements ConversationHistoryService {

    private static final String KEY_PREFIX = "chat:history:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, String>> getHistory(Long memberId) {
        String key = buildKey(memberId);
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);

        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }

        return raw.stream()
                .map(item -> {
                    try {
                        return objectMapper.readValue((String) item, new TypeReference<Map<String, String>>() {});
                    } catch (Exception e) {
                        log.error("히스토리 역직렬화 실패: {}", item, e);
                        return null;
                    }
                })
                .filter(item -> item != null)
                .toList();
    }

    @Override
    public void saveMessage(Long memberId, String role, String content) {
        String key = buildKey(memberId);

        try {
            String json = objectMapper.writeValueAsString(Map.of("role", role, "content", content));
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, TTL);
        } catch (Exception e) {
            log.error("히스토리 저장 실패 - memberId: {}", memberId, e);
        }
    }

    @Override
    public void clearHistory(Long memberId) {
        String key = buildKey(memberId);
        redisTemplate.delete(key);
        log.debug("히스토리 삭제 완료 - key: {}", key);
    }

    private String buildKey(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
