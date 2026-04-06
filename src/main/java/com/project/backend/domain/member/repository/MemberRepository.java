package com.project.backend.domain.member.repository;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 삭제되지 않은 활성 회원 조회
    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Member> findActiveById(@Param("id") Long id);

    // 삭제된 회원 포함 조회 (스케줄러용)
    @Query("SELECT m FROM Member m WHERE m.deletedAt IS NOT NULL AND m.deletedAt < :threshold")
    List<Member> findAllDeletedBefore(@Param("threshold") LocalDateTime threshold);


    @Query("""
    SELECT m.id
    FROM Member m
    JOIN Setting s ON s.member = m
    WHERE m.deletedAt IS NULL
      AND s.suggestion = TRUE
""")
    List<Long> findActiveMemberIdsWithSuggestionEnabled();

    // Auth 정보와 함께 활성 회원 조회
    @Query("SELECT m FROM Member m JOIN FETCH m.auth WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Member> findActiveByIdWithAuth(@Param("id") Long id);

    // 친구 피요청자의 객체 조회 (역할이 유저이며, 세션 멤버 자기 자신이 아닌)
    Optional<Member> findByEmailAndRoleAndIdNot(String email, Role role, Long memberId);

}
