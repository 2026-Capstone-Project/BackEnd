package com.project.backend.domain.todo.repository;

import com.project.backend.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Modifying
    @Query("DELETE FROM Todo t WHERE t.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
