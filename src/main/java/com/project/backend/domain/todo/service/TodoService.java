package com.project.backend.domain.todo.service;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.todo.converter.TodoConverter;
import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.entity.RecurringTodo;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.repository.RecurringTodoRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final RecurringTodoRepository recurringTodoRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public TodoResDTO.TodoInfo createTodo(TodoReqDTO.CreateTodo reqDTO, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        RecurringTodo recurringTodo = null;

        // 반복 할일인 경우 RecurringTodo 먼저 생성
        if (reqDTO.recurrence() != null) {
            recurringTodo = TodoConverter.toRecurringTodo(reqDTO, member);
            recurringTodo = recurringTodoRepository.save(recurringTodo);
        }

        // Todo 생성
        Todo todo = TodoConverter.toTodo(reqDTO, member, recurringTodo);
        todo = todoRepository.save(todo);

        return TodoConverter.toTodoInfo(todo);
    }
}
