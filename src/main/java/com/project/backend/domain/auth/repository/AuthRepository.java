package com.project.backend.domain.auth.repository;

import com.project.backend.domain.auth.entity.Auth;
import com.project.backend.domain.auth.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {

    @Query("SELECT a FROM Auth a JOIN FETCH a.member WHERE a.provider = :provider AND a.providerId = :providerId")
    Optional<Auth> findByProviderAndProviderIdWithMember(@Param("provider") Provider provider, @Param("providerId") String providerId);

    void deleteByMemberId(Long memberId);

    @Query("""
         SELECT a
         FROM Auth a
         WHERE a.provider =:provider AND a.providerId =:providerId
    """)
    Optional<Auth> findByProviderAndProviderId(
            @Param("provider") Provider provider,
            @Param("providerId") String providerId
    );

    /**
     * 탈퇴한 회원의 Auth 조회 (재가입 제한 검증용)
     */
    @Query("""
        SELECT a FROM Auth a
        JOIN FETCH a.member m
        WHERE a.provider = :provider
          AND a.providerId = :providerId
          AND m.deletedAt IS NOT NULL
    """)
    Optional<Auth> findDeletedByProviderAndProviderId(
            @Param("provider") Provider provider,
            @Param("providerId") String providerId
    );
}
