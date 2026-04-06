package com.project.backend.domain.friend.repository;

import com.project.backend.domain.friend.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    // 친구 요청 존재 여부
    boolean existsBySenderIdAndReceiverId(Long memberId, Long friendId);

    // 친구 요청 객체 조회
    Optional<FriendRequest> findBySenderIdAndReceiverId(Long memberId, Long friendId);

    // senderId로 객체 조회
    List<FriendRequest> findBySenderId(Long memberId);
}
