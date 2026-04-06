package com.project.backend.domain.friend.repository;

import com.project.backend.domain.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    // memberId, friendId로 친구 객체 찾기
    boolean existsByMemberIdAndFriendId(Long memberId, Long friendId);

    // memberId로 모든 친구 객체 찬기
    List<Friend> findByMemberId(Long memberId);

}
