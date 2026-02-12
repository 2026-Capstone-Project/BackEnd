package com.project.backend.domain.suggestion.repository;

import com.project.backend.domain.suggestion.entity.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

    @Modifying
    @Query("DELETE FROM Suggestion s WHERE s.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    List<Suggestion> findByMemberIdAndActiveIsTrueOrderByIdDesc(Long memberId);

    Optional<Suggestion> findByIdAndActiveIsTrue(Long suggestionId);

    @Query("""
        select s.targetKeyHash
        from Suggestion s
        where s.member.id = :memberId
          and s.active = true
          and s.targetKeyHash in :hashes
    """)
    List<byte[]> findExistingActiveTargetKeyHashes(
            @Param("memberId") Long memberId,
            @Param("hashes") Collection<byte[]> hashes
    );
}
