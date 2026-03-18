package com.project.backend.domain.event.repository;

import com.project.backend.domain.event.entity.EventTitleHistory;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EventTitleHistoryRepository extends JpaRepository<EventTitleHistory, Long> {

    @Query("SELECT h " +
            "FROM EventTitleHistory h " +
            "WHERE h.memberId = :memberId AND h.title = :title")
    Optional<EventTitleHistory> findByMemberIdAndTitle(
            @Param("memberId") Long memberId,
            @Param("title") String title
    );
}
