package com.project.backend.domain.member.controller;

import com.project.backend.domain.member.controller.docs.MemberDocs;
import com.project.backend.domain.member.service.MemberService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberDocs {

    private final MemberService memberService;

    @Override
    @DeleteMapping("")
    public CustomResponse<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        memberService.withdraw(userDetails.getId(), request, response);
        return CustomResponse.onSuccess("회원 탈퇴가 완료되었습니다.", null);
    }
}
