package com.project.backend.domain.todo.converter;

import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.entity.TodoTitleHistory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TodoHistoryConverter {

    public static TodoTitleHistory toTodoTitleHistory(Long memberId, String title) {
        return TodoTitleHistory.builder()
                .title(title)
                .lastUsedAt(LocalDateTime.now())
                .memberId(memberId)
                .build();
    }

    public static TodoResDTO.TodoTitleHistoryRes toTodoTitleHistoryRes(List<String> titleHistory) {
        return TodoResDTO.TodoTitleHistoryRes.builder()
                .titleHistory(titleHistory)
                .build();
    }
}
