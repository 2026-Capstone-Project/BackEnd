package com.project.backend.domain.friend.controller;

import com.project.backend.domain.friend.dto.request.FriendReqDTO;
import com.project.backend.domain.friend.dto.response.FriendResDTO;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "친구 API", description = "친구 요청 및 친구 관계 관리 API")
public interface FriendDocs {

    @Operation(
            summary = "보낸 친구 요청 목록 조회",
            description = "인증된 사용자가 보낸 친구 요청 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "보낸 친구 요청 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class)
                    )
            )
    })
    CustomResponse<FriendResDTO.FriendRequestListRes> getSentFriendRequest(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "받은 친구 요청 목록 조회",
            description = "인증된 사용자가 받은 친구 요청 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "받은 친구 요청 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class)
                    )
            )
    })
    CustomResponse<FriendResDTO.FriendRequestListRes> getReceivedFriendRequest(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "친구 목록 조회",
            description = "인증된 사용자의 친구 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "친구 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class)
                    )
            )
    })
    CustomResponse<FriendResDTO.FriendListRes> getFriend(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "친구 요청 전송",
            description = """
                    이메일로 다른 사용자에게 친구 요청을 전송합니다.

                    - 이미 친구인 경우 409를 반환합니다.
                    - 이미 친구 요청을 보낸 경우 409를 반환합니다.
                    - 상대방이 나에게 보낸 친구 요청이 이미 있으면 자동으로 친구가 됩니다.
                    """
    )
    @RequestBody(
            required = true,
            description = "친구 요청 대상 이메일",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = FriendReqDTO.SendRequestReq.class)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "친구 요청 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "친구 요청 성공 예시",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON201",
                                              "message": "친구 요청 완료",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 친구 / 이미 요청됨",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "ALREADY_FRIEND",
                                            summary = "이미 친구인 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "FRIEND409_1",
                                                      "message": "이미 친구입니다.",
                                                      "result": null
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "ALREADY_REQUESTED",
                                            summary = "이미 친구 요청을 보낸 경우",
                                            value = """
                                                    {
                                                      "isSuccess": false,
                                                      "code": "FRIEND409_2",
                                                      "message": "이미 친구 요청이 전송되었습니다",
                                                      "result": null
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    CustomResponse<String> sendRequest(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,
            @org.springframework.web.bind.annotation.RequestBody @Valid FriendReqDTO.SendRequestReq reqDTO
    );

    @Operation(
            summary = "친구 요청 수락",
            description = """
                    받은 친구 요청을 수락합니다.

                    - 친구 요청의 receiver 본인만 수락할 수 있습니다.
                    - 수락 시 양방향 친구 관계가 생성되고 기존 친구 요청은 삭제됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "친구 요청 수락 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "친구 요청 수락 성공 예시",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "친구 요청 수락 완료",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 친구 요청에 대한 접근 권한이 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "FRIEND_REQUEST_FORBIDDEN",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "FRIEND403_1",
                                              "message": "해당 친구 요청에 대한 접근 권한이 없습니다",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "친구 요청을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "FRIEND_REQUEST_NOT_FOUND",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "FRIEND404_1",
                                              "message": "해당 친구 요청을 찾을 수 없습니다",
                                              "result": null
                                            }
                                            """
                            )
                    )
            )
    })
    CustomResponse<String> acceptFriendRequest(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @Parameter(
                    description = "수락할 친구 요청 ID",
                    required = true
            )
            @PathVariable Long friendRequestId
    );

    @Operation(
            summary = "친구 요청 거절",
            description = """
                    받은 친구 요청을 거절합니다.

                    - 친구 요청의 receiver 본인만 거절할 수 있습니다.
                    - 거절 시 친구 요청은 삭제됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "친구 요청 거절 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "친구 요청 거절 성공 예시",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "친구 요청 거절 완료",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 친구 요청에 대한 접근 권한이 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "FRIEND_REQUEST_FORBIDDEN",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "FRIEND403_1",
                                              "message": "해당 친구 요청에 대한 접근 권한이 없습니다",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "친구 요청을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "FRIEND_REQUEST_NOT_FOUND",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "FRIEND404_1",
                                              "message": "해당 친구 요청을 찾을 수 없습니다",
                                              "result": null
                                            }
                                            """
                            )
                    )
            )
    })
    CustomResponse<String> rejectFriendRequest(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @Parameter(
                    description = "거절할 친구 요청 ID",
                    required = true
            )
            @PathVariable Long friendRequestId
    );

    @Operation(
            summary = "친구 삭제",
            description = """
                    친구 관계를 삭제합니다.

                    - 현재 사용자가 소유한 친구 관계만 삭제할 수 있습니다.
                    - 삭제 시 상대방의 친구 관계도 함께 삭제됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "친구 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "친구 삭제 성공 예시",
                                    value = """
                                            {
                                              "isSuccess": true,
                                              "code": "COMMON200",
                                              "message": "친구 삭제 완료",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "해당 친구에 대한 접근 권한이 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "FRIEND_FORBIDDEN",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "FRIEND403_2",
                                              "message": "해당 친구에 대한 접근 권한이 없습니다",
                                              "result": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "친구를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CustomResponse.class),
                            examples = @ExampleObject(
                                    name = "FRIEND_NOT_FOUND",
                                    value = """
                                            {
                                              "isSuccess": false,
                                              "code": "FRIEND404_2",
                                              "message": "해당 친구를 찾을 수 없습니다",
                                              "result": null
                                            }
                                            """
                            )
                    )
            )
    })
    CustomResponse<String> deleteFriend(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            CustomUserDetails customUserDetails,

            @Parameter(
                    description = "삭제할 친구 ID",
                    required = true
            )
            @PathVariable Long friendId
    );
}