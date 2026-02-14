package com.project.backend.domain.suggestion.executor;

import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Category;
import com.project.backend.domain.suggestion.enums.Status;

/**
 * 입력받은 Suggestion 객체를 실행하여 실제 서비스 객체로 만드는 Executor
 */
public interface SuggestionExecutor {
    /**
     * @param suggestion    제안 객체
     * @param currentStatus 제안 객체를 처음으로 읽은 순간의 상태 (한 번 읽은 제안은 만료되서 상태를 다시 호출하여 쓸 수 없음)
     * @param memberId      제안 실행 시 이미 제안을 생성할 자리에 객체가 존재하는지 판단하기 위한 쿼리용
     */
    void execute(Suggestion suggestion, Status currentStatus, Long memberId);

    // TODO : 나중에 SuggestionExecutorFactory에 Executor가 많아지면 사용할 support 메서드
    boolean supports(Category category);
}
