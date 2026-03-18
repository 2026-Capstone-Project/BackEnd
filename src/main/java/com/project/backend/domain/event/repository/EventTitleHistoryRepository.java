package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.EventTitleHistory;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EventTitleHistoryRepository extends JpaRepository<EventTitleHistory, Long> {

    @Query("SELECT h " +
            "FROM EventTitleHistory h " +
            "WHERE h.memberId = :memberId AND h.title = :title")
    Optional<EventTitleHistory> findByMemberIdAndTitle(
            @Param("memberId") Long memberId,
            @Param("title") String title
    );

    @Query("SELECT h.title " +
            "FROM EventTitleHistory h " +
            "WHERE h.memberId = :memberId " +
            "ORDER BY h.lastUsedAt desc " +
            "LIMIT 5")
    List<String> findTitleHistoryByMemberId(
            @Param("memberId") Long memberId
    );

    @Query("SELECT r.title " +
            "FROM (" +
                "SELECT h.title title, h.lastUsedAt lastUsedAt " +
                "FROM EventTitleHistory h " +
                "WHERE h.memberId = :memberId AND h.title LIKE CONCAT('%', :keyword, '%') " +
                "ORDER BY h.lastUsedAt desc " +
                "LIMIT 5 " +
            ") r " +
            "ORDER BY " +
                "CASE " +
                    "WHEN r.title LIKE CONCAT(:keyword, '%') THEN 0" +
                    " ELSE 1 " +
                "END, " +
                "r.lastUsedAt DESC")
    List<String> findTitleHistoryByMemberIdAndKeyword(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword
    );
}
