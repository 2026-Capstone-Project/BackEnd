package com.project.backend.domain.todo.repository;

import com.project.backend.domain.todo.entity.TodoTitleHistory;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TodoTitleHistoryRepository extends JpaRepository<TodoTitleHistory, Long> {

    /**
     * 멤버의 히스토리 조회 (저장시 사용)
     */
    @Query("SELECT h " +
            "FROM TodoTitleHistory h " +
            "WHERE h.memberId = :memberId AND h.title = :title")
    Optional<TodoTitleHistory> findByMemberIdAndTitle(
            @Param("memberId") Long memberId,
            @Param("title") String title
    );

    /**
     * 가장 최신 5개 제목 조회 (requestParam(keyword)가 널인경우)
     */
    @Query("SELECT h.title " +
            "FROM TodoTitleHistory h " +
            "WHERE h.memberId = :memberId " +
            "ORDER BY h.lastUsedAt desc " +
            "LIMIT 5")
    List<String> findTitleHistoryByMemberId(
            @Param("memberId") Long memberId
    );

    /**
     * 가장 최신 5개 제목중에서 prefix가 keyword라면 우선순위를 부여하여 조회
     */
    @Query("SELECT r.title " +
            "FROM (" +
                "SELECT h.title title, h.lastUsedAt lastUsedAt " +
                "FROM TodoTitleHistory h " +
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
