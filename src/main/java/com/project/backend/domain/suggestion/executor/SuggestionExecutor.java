package com.project.backend.domain.suggestion.executor;

import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;

/**
 * 입력받은 Suggestion 객체를 실행하여 실제 서비스 객체로 만드는 Executor
 */
public interface SuggestionExecutor {
    /**
     * @param suggestion 제안 객체
     * @param currentStatus 제안 객체를 처음으로 읽은 순간의 상태 (한 번 읽은 제안은 만료되서 상태를 다시 호출하여 쓸 수 없음)
     */
    void execute(Suggestion suggestion, Status currentStatus);

    // TODO : 나중에 SuggestionExecutorFactory에 Executor가 많아지면 사용할 support 메서드
    boolean supports(Category category);
}
