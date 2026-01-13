package com.project.backend.domain.auth.repository;

import com.project.backend.domain.auth.entity.Auth;
import com.project.backend.domain.auth.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {

    Optional<Auth> findByProviderAndProviderId(Provider provider, String providerId);

    @Query("SELECT a FROM Auth a JOIN FETCH a.member WHERE a.provider = :provider AND a.providerId = :providerId")
    Optional<Auth> findByProviderAndProviderIdWithMember(@Param("provider") Provider provider, @Param("providerId") String providerId);

    void deleteByMemberId(Long memberId);
}
