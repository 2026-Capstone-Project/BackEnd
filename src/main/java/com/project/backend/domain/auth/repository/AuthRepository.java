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
    @Query("""
         SELECT a
         FROM Auth a
         WHERE a.provider =:provider AND a.providerId =:providerId
    """)
    Optional<Auth> findByProviderAndProviderId(
            @Param("provider") Provider provider,
            @Param("providerId") String providerId
    );
}
