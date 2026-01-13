package com.project.backend.domain.auth.converter;

import com.project.backend.domain.auth.enums.Provider;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * URL PathVariable에서 소문자 Provider 이름을 Enum으로 변환
 * 예: "kakao" -> Provider.KAKAO
 */
@Component
public class ProviderConverter implements Converter<String, Provider> {

    @Override
    public Provider convert(String source) {
        return Provider.valueOf(source.toUpperCase());
    }
}
