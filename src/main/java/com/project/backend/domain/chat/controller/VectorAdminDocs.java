package com.project.backend.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "벡터 관리 API (Admin)", description = "Qdrant 벡터 DB 관리 API — 운영 환경에서 신중하게 사용")
public interface VectorAdminDocs {

    @Operation(
            summary = "벡터 전체 재동기화",
            description = """
                    Qdrant 컬렉션을 초기화한 뒤 DB의 모든 Event와 Todo를 재인덱싱합니다.

                    **실행 순서**
                    1. Qdrant 컬렉션 전체 초기화 (기존 벡터 전부 삭제)
                    2. DB의 Event 전체 임베딩 후 Qdrant upsert
                    3. DB의 Todo 전체 임베딩 후 Qdrant upsert

                    **주의사항**
                    - 실행 중에는 Qdrant 컬렉션이 비어 있는 상태가 되므로 **AI 채팅의 벡터 검색 기능이 일시적으로 동작하지 않습니다.**
                    - 개별 Event/Todo 임베딩 실패는 로그만 남기고 계속 진행됩니다. 반환된 건수와 DB 전체 건수가 다를 경우 서버 로그를 확인하세요.
                    - 데이터가 많을수록 응답까지 시간이 오래 걸릴 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "재동기화 완료",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    name = "재동기화 성공",
                                    value = "재동기화 완료: 128건"
                            )
                    )
            )
    })
    ResponseEntity<String> resync();
}
