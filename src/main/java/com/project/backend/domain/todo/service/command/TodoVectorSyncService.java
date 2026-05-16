package com.project.backend.domain.todo.service.command;

import com.project.backend.domain.common.enums.VectorSyncStatus;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.TodoRepository;
import com.project.backend.domain.nlp.client.EmbeddingClient;
import com.project.backend.global.qdrant.client.QdrantVectorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoVectorSyncService {

    private static final long TODO_ID_OFFSET = 2_000_000_000L;

    private final TodoRepository todoRepository;
    private final EmbeddingClient embeddingClient;
    private final QdrantVectorClient qdrantVectorClient;

    @Async("vectorSyncExecutor")
    @Transactional
    public void syncOnCreate(Long todoId) {
        Todo todo = todoRepository.findWithMemberById(todoId)
                .orElseThrow(() -> new IllegalStateException("벡터 동기화 대상 Todo 없음 - todoId: " + todoId));
        doUpsertSync(todo);
    }

    @Async("vectorSyncExecutor")
    @Transactional
    public void syncOnUpdate(Long todoId) {
        Todo todo = todoRepository.findWithMemberById(todoId)
                .orElseThrow(() -> new IllegalStateException("벡터 동기화 대상 Todo 없음 - todoId: " + todoId));
        doUpsertSync(todo);
    }

    @Async("vectorSyncExecutor")
    public void syncOnDelete(Long todoId) {
        try {
            qdrantVectorClient.delete(todoId + TODO_ID_OFFSET);
            log.debug("Qdrant 삭제 완료 - todoId: {}", todoId);
        } catch (Exception e) {
            log.error("Qdrant 삭제 실패 - todoId: {}", todoId, e);
        }
    }

    private void doUpsertSync(Todo todo) {
        try {
            float[] vector = embeddingClient.embed(buildEmbedText(todo));
            qdrantVectorClient.upsert(
                    todo.getId() + TODO_ID_OFFSET,
                    todo.getMember().getId(),
                    todo.getTitle(),
                    todo.getStartDate().toString(),
                    "TODO",
                    todo.getTodoRecurrenceGroup() != null,
                    vector
            );
            todo.updateVectorSyncStatus(VectorSyncStatus.SUCCESS);
            log.debug("Qdrant 동기화 완료 - todoId: {}", todo.getId());
        } catch (Exception e) {
            log.error("Qdrant 동기화 실패 - todoId: {}", todo.getId(), e);
            todo.updateVectorSyncStatus(VectorSyncStatus.FAILED);
        }
    }

    private String buildEmbedText(Todo todo) {
        StringBuilder sb = new StringBuilder(todo.getTitle());
        if (todo.getMemo() != null) {
            sb.append(" ").append(todo.getMemo());
        }
        return sb.toString();
    }
}