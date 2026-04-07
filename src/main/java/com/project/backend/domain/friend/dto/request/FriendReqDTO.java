package com.project.backend.domain.friend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class FriendReqDTO {

    public record SendRequestReq(
            @NotBlank
            String name,

            @Email
            @NotBlank
            String email
    ) {
    }
}
