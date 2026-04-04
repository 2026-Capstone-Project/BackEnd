package com.project.backend.domain.nlp.client;

/**
 * OpenAI Function Calling 응답 파싱 결과.
 *
 * nlp 패키지 위치 이유:
 *   LLM API 응답 모델 → LlmClient(nlp)가 반환 타입으로 사용.
 *   chat 패키지에 두면 nlp → chat 순환 의존 발생.
 *
 * @param textContent       일반 텍스트 응답 (function call이 아닐 때만 값 존재, 나머지는 null)
 * @param functionName      호출된 함수명 (일반 텍스트 응답이면 null)
 * @param toolCallId        OpenAI 부여 ID → 2차 호출 시 tool_call_id로 필수
 * @param functionArguments LLM이 추출한 파라미터 JSON string
 */
public record FunctionCallResponse(
        String textContent,
        String functionName,
        String toolCallId,
        String functionArguments
) {
    /** CRUD 함수 호출 여부 (askForClarification, respondToUser 제외) */
    public boolean isFunctionCall() {
        return functionName != null
                && !"askForClarification".equals(functionName)
                && !"respondToUser".equals(functionName);
    }

    /** 일반 텍스트 응답 함수 호출 여부 */
    public boolean isRespondToUser() {
        return "respondToUser".equals(functionName);
    }

    /** 되묻기 함수 호출 여부 */
    public boolean isClarification() {
        return "askForClarification".equals(functionName);
    }

    /** 일반 텍스트 응답 여부 */
    public boolean isText() {
        return functionName == null;
    }
}