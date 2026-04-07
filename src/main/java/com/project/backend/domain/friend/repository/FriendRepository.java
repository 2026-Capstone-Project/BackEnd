package com.project.backend.domain.friend.repository;

import com.project.backend.domain.friend.entity.Friend;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    // memberId, friendId로 친구 객체 찾기
    boolean existsByMemberIdAndOpponentId(Long memberId, Long friendId);

    // memberId로 모든 친구 객체 찬기
    List<Friend> findByMemberId(Long memberId);

    // memberId와 OpponentId로 친구 찾기
    Optional<Friend> findByMemberIdAndOpponentId(Long memberId, Long friendId);

    // 입력한 keyword와 유사한 이름을 가진 친구 객체 찾기
    @Query("SELECT f " +
            "FROM Friend f " +
            "JOIN Member m ON m.id = f.opponent.id " +
            "WHERE f.member.id = :memberId " +
            "AND m.nickname LIKE CONCAT('%', :keyword, '%') " +
            "ORDER BY " +
                "CASE " +
                    "WHEN m.nickname LIKE CONCAT(:keyword, '%') THEN 0 " +
                    "ELSE 1 " +
                "END, " +
                "m.nickname ASC " +
            "LIMIT 20"
    )
    List<Friend> findByMemberIdAndKeyword(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword
    );
}
