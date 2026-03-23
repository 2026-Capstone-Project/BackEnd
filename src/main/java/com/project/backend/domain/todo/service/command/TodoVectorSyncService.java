package com.project.backend.domain.todo.service.command;

import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.nlp.client.EmbeddingClient;
import com.project.backend.global.qdrant.client.QdrantVectorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoVectorSyncService {

    // Event ID와 충돌 방지를 위한 오프셋
    // Todo Qdrant point ID = todoId + TODO_ID_OFFSET
    private static final long TODO_ID_OFFSET = 2_000_000_000L;

    private final EmbeddingClient embeddingClient;
    private final QdrantVectorClient qdrantVectorClient;

    public void syncOnCreate(Todo todo) {
        try {
            float[] vector = embeddingClient.embed(buildEmbedText(todo));
            qdrantVectorClient.upsert(
                    todo.getId() + TODO_ID_OFFSET,
                    todo.getMember().getId(),
                    todo.getTitle(),
                    todo.getStartDate().toString(),
                    "TODO",
                    vector
            );
            log.debug("Qdrant 저장 완료 - todoId: {}", todo.getId());
        } catch (Exception e) {
            log.error("Qdrant 동기화 실패 (create) - todoId: {}", todo.getId(), e);
        }
    }

    public void syncOnUpdate(Todo todo) {
        syncOnCreate(todo);
    }

    public void syncOnDelete(Long todoId) {
        try {
            qdrantVectorClient.delete(todoId + TODO_ID_OFFSET);
            log.debug("Qdrant 삭제 완료 - todoId: {}", todoId);
        } catch (Exception e) {
            log.error("Qdrant 동기화 실패 (delete) - todoId: {}", todoId, e);
        }
    }

    // 임베딩할 텍스트: title + memo 조합
    private String buildEmbedText(Todo todo) {
        StringBuilder sb = new StringBuilder(todo.getTitle());
        if (todo.getMemo() != null) {
            sb.append(" ").append(todo.getMemo());
        }
        return sb.toString();
    }
}
