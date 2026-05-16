package com.project.backend.domain.chat.function;

import com.project.backend.domain.chat.enums.ActionType;
import com.project.backend.domain.chat.enums.ScheduleType;

/**
 * FunctionCallHandler 서비스 실행 결과.
 * FunctionCallResponse(LLM 레벨)와 구분되는 비즈니스 레벨 결과 모델.
 *
 * @param scheduleId        생성/수정/삭제된 단건 ID → 프론트 단건 갱신용
 * @param recurrenceGroupId 반복 일정일 때만 존재, null 가능 → 프론트 전체 그룹 갱신용
 * @param summary           LLM 재호출에 넘길 실행 결과 한국어 요약
 */
public record ScheduleActionResult(
        ActionType action,
        ScheduleType scheduleType,
        Long scheduleId,
        Long recurrenceGroupId,
        String summary
) {}