package com.project.backend.domain.nlp.service;

import com.project.backend.domain.nlp.dto.request.NlpReqDTO;
import com.project.backend.domain.nlp.dto.response.NlpResDTO;

public interface NlpService {
    NlpResDTO.ParseRes parse(NlpReqDTO.ParseReq reqDTO, Long memberId);
    NlpResDTO.ConfirmRes confirm(NlpReqDTO.ConfirmReq reqDTO, Long memberId);
}
