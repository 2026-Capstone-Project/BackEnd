package com.project.backend.domain.chat.service;

import com.project.backend.domain.event.entity.Event;
import com.project.backend.domain.event.repository.EventRepository;
import com.project.backend.domain.nlp.client.EmbeddingClient;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.TodoRepository;
import com.project.backend.global.qdrant.client.QdrantVectorClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorReSyncService {

    private static final long TODO_ID_OFFSET = 2_000_000_000L;

    private final QdrantVectorClient qdrantVectorClient;
    private final EmbeddingClient embeddingClient;
    private final EventRepository eventRepository;
    private final TodoRepository todoRepository;

    @Transactional(readOnly = true)
    public int resyncAll() {
        qdrantVectorClient.resetCollection();

        List<Event> events = eventRepository.findAllWithMember();
        List<Todo> todos = todoRepository.findAllWithMember();

        int count = 0;

        for (Event event : events) {
            try {
                float[] vector = embeddingClient.embed(buildEventEmbedText(event));
                qdrantVectorClient.upsert(
                        event.getId(),
                        event.getMember().getId(),
                        event.getTitle(),
                        event.getStartTime().toLocalDate().toString(),
                        "EVENT",
                        event.getRecurrenceGroup() != null,
                        vector
                );
                count++;
            } catch (Exception e) {
                log.error("Event 재동기화 실패 - eventId: {}", event.getId(), e);
            }
        }

        for (Todo todo : todos) {
            try {
                float[] vector = embeddingClient.embed(buildTodoEmbedText(todo));
                qdrantVectorClient.upsert(
                        todo.getId() + TODO_ID_OFFSET,
                        todo.getMember().getId(),
                        todo.getTitle(),
                        todo.getStartDate().toString(),
                        "TODO",
                        todo.getTodoRecurrenceGroup() != null,
                        vector
                );
                count++;
            } catch (Exception e) {
                log.error("Todo 재동기화 실패 - todoId: {}", todo.getId(), e);
            }
        }

        log.info("벡터 전체 재동기화 완료 - 총 {}건", count);
        return count;
    }

    private String buildEventEmbedText(Event event) {
        StringBuilder sb = new StringBuilder(event.getTitle());
        if (event.getContent() != null) sb.append(" ").append(event.getContent());
        if (event.getLocation() != null) sb.append(" ").append(event.getLocation());
        return sb.toString();
    }

    private String buildTodoEmbedText(Todo todo) {
        StringBuilder sb = new StringBuilder(todo.getTitle());
        if (todo.getMemo() != null) sb.append(" ").append(todo.getMemo());
        return sb.toString();
    }
}
