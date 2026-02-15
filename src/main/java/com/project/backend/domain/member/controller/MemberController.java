package com.project.backend.domain.member.controller;

import com.project.backend.domain.member.controller.docs.MemberDocs;
import com.project.backend.domain.member.dto.response.MemberResDTO;
import com.project.backend.domain.member.service.MemberService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberDocs {

    private final MemberService memberService;

    @Override
    @GetMapping("/me")
    public CustomResponse<MemberResDTO.MyInfo> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MemberResDTO.MyInfo resDTO = memberService.getMyInfo(userDetails.getId());
        return CustomResponse.onSuccess("내 정보 조회 성공", resDTO);
    }

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
