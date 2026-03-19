package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.EventLocationHistory;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EventLocationHistoryRepository extends JpaRepository<EventLocationHistory, Long> {

    @Query("SELECT h " +
            "FROM EventLocationHistory h " +
            "WHERE h.memberId = :memberId AND h.location = :location")
    Optional<EventLocationHistory> findByMemberIdAndLocation(
            @Param("memberId") Long memberId,
            @Param("location") String location
    );

    @Query("SELECT h.location " +
            "FROM EventLocationHistory h " +
            "WHERE h.memberId = :memberId " +
            "ORDER BY h.lastUsedAt desc " +
            "LIMIT 5")
    List<String> findLocationHistoryByMemberId(
            @Param("memberId") Long memberId
    );

    @Query("SELECT r.location " +
            "FROM (" +
            "SELECT h.location location, h.lastUsedAt lastUsedAt " +
            "FROM EventLocationHistory h " +
            "WHERE h.memberId = :memberId AND h.location LIKE CONCAT('%', :keyword, '%') " +
            "ORDER BY h.lastUsedAt desc " +
            "LIMIT 5 " +
            ") r " +
            "ORDER BY " +
            "CASE " +
            "WHEN r.location LIKE CONCAT(:keyword, '%') THEN 0" +
            " ELSE 1 " +
            "END, " +
            "r.lastUsedAt DESC")
    List<String> findLocationHistoryByMemberIdAndKeyword(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword
    );
}
