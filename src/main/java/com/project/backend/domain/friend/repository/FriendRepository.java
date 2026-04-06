package com.project.backend.domain.friend.repository;

import com.project.backend.domain.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // memberId와 OpponentId로 친구 여부 카운트
    long countByMemberIdAndOpponentIdIn(Long memberId, List<Long> participantIds);
}
