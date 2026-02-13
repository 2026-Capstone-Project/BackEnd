package com.project.backend.domain.suggestion.repository;

import com.project.backend.domain.suggestion.entity.Suggestion;
import com.project.backend.domain.suggestion.enums.Status;
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

    // 어차피 조인할 내용을 한번에 가져와서 N+1 방지
    @Query("""
        select s from Suggestion s
        left join fetch s.previousEvent
        left join fetch s.previousTodo
        left join fetch s.recurrenceGroup
        left join fetch s.todoRecurrenceGroup
        where s.id = :id and s.member.id = :memberId
    """)
    Optional<Suggestion> findForExecute(@Param("id") Long id, @Param("memberId") Long memberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Suggestion s
           set s.status = com.project.backend.domain.suggestion.enums.Status.ACCEPTED,
               s.active = NULL
         where s.id = :id
           and s.member.id = :memberId
           and s.active = true
           and s.status = :currentStatus
    """)
    int acceptAtomically(
            @Param("id") Long id,
            @Param("memberId") Long memberId,
            @Param("currentStatus") Status currentStatus
    );

    // secondary가 널이 아닌데, primary 상태라면 secondary로 전환
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Suggestion s
           set s.status = com.project.backend.domain.suggestion.enums.Status.SECONDARY
         where s.id = :suggestionId
           and s.member.id = :memberId
           and s.active = true
           and s.status = com.project.backend.domain.suggestion.enums.Status.PRIMARY
           and s.secondaryContent is not null
    """)
    int rejectPrimaryToSecondary(@Param("memberId") Long memberId,
                                 @Param("suggestionId") Long suggestionId);

    // primary 상태에서 secondary가 널인 상태 그리고, secondary인 상태에서 거절했을 때
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Suggestion s
           set s.status = com.project.backend.domain.suggestion.enums.Status.REJECTED,
               s.active = NULL
         where s.id = :suggestionId
           and s.member.id = :memberId
           and s.active = true
           and s.status in (
                com.project.backend.domain.suggestion.enums.Status.PRIMARY,
                com.project.backend.domain.suggestion.enums.Status.SECONDARY
           )
    """)
    int rejectFinally(@Param("memberId") Long memberId,
                      @Param("suggestionId") Long suggestionId);

    Optional<Suggestion> findByIdAndMember_IdAndActiveIsTrue(Long id, Long memberId);
}
