package com.project.backend.domain.member.service;

import com.project.backend.domain.auth.dto.response.AuthResDTO;
import com.project.backend.domain.member.converter.MemberConverter;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.setting.converter.SettingConverter;
import com.project.backend.domain.setting.entity.Setting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public Member createMember(AuthResDTO.UserAuth userAuth){

        Member member = MemberConverter.toMember(userAuth);
        Setting setting = SettingConverter.toSetting(member);
        member.updateSetting(setting);
        return memberRepository.save(member);
    }
}
