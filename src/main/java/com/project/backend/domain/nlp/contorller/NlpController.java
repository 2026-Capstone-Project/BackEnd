package com.project.backend.domain.nlp.contorller;

import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.dto.response.NlpResDTO;
import com.project.backend.domain.nlp.service.NlpService;
import com.project.backend.global.apiPayload.CustomResponse;
import com.project.backend.global.security.userdetails.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/nlp")
@RequiredArgsConstructor
public class NlpController implements NlpDocs{

    private final NlpService nlpService;

    @Override
    @PostMapping("/parse")
    public CustomResponse<NlpResDTO.ParseRes> parse(@RequestBody @Valid NlpReqDTO.ParseReq reqDTO,
                                                    @AuthenticationPrincipal CustomUserDetails customUserDetails) {

        NlpResDTO.ParseRes resDTO = nlpService.parse(reqDTO, customUserDetails.getId());
        return CustomResponse.onSuccess("파싱 성공", resDTO);
    }

    @Override
    @PostMapping("/confirm")
    public CustomResponse<NlpResDTO.ConfirmRes> confirm(@RequestBody @Valid NlpReqDTO.ConfirmReq reqDTO,
                                                        @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        NlpResDTO.ConfirmRes resDTO = nlpService.confirm(reqDTO, customUserDetails.getId());
        return CustomResponse.onSuccess("저장 성공", resDTO);
    }
}
