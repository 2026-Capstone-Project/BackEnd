package com.project.backend.domain.suggestion.repository;

import com.project.backend.domain.suggestion.entity.Suggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

    @Modifying
    @Query("DELETE FROM Suggestion s WHERE s.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    List<Suggestion> findByMemberIdAndActiveIsTrueOrderByIdDesc(Long memberId);
}
