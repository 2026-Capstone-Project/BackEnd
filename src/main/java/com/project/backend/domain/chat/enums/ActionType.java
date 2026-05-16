package com.project.backend.domain.chat.enums;

public enum ActionType {
    CREATED,    // 일정/할 일 생성 완료
    UPDATED,    // 일정/할 일 수정 완료
    DELETED,    // 일정/할 일 삭제 완료
    CLARIFYING, // 되묻는 중 — 프론트 별도 UI 처리 가능
    NONE        // 일반 답변, DB 변화 없음
}
