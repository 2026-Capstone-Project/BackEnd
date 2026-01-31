package com.project.backend.domain.todo.repository;

import com.project.backend.domain.todo.entity.RecurringTodo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTodoRepository extends JpaRepository<RecurringTodo, Long> {
}
