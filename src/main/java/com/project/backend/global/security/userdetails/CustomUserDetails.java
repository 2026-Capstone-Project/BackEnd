package com.project.backend.global.security.userdetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.backend.domain.auth.enums.Provider;
import com.project.backend.domain.member.enums.Role;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    @Getter
    private final Long id;
    private final String username;
    private final Role role;

    public CustomUserDetails(Long id, String providerId , Role role) {
        this.id = id;
        this.username = providerId;
        this.role = role;
    }

    @Override
    @NonNull
    public String getUsername() {
        return username;
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    // 이 서비스에서는 비밀번호가 없으므로 null 리턴
    @Override
    public @Nullable String getPassword() {
        return null;
    }

}
