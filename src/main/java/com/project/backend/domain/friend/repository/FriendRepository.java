package com.project.backend.domain.friend.repository;

import com.project.backend.domain.friend.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // 해당 맴버 아이디를 가진 사람과 친구인 사람들의 id를 조회
    @Query("""
    select f.opponent.id
    from Friend f
    where f.opponent.id in :friendIds
      and f.member.id = :memberId
""")
    List<Long> findOpponentMemberIdsByFriendIdsAndMemberId(@Param("friendIds") List<Long> friendIds,
                                                           @Param("memberId") Long memberId);
}
