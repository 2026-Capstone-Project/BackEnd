package com.project.backend.domain.event.dto.response.swagger;

import com.project.backend.domain.event.dto.response.EventResDTO;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EventDetailResponse")
public class EventDetailRes {

    @Schema(example = "true")
    public boolean isSuccess;

    @Schema(example = "200")
    public String code;

    @Schema(example = "OK")
    public String message;

    public EventResDTO.DetailRes result;
}