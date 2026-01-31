package com.project.backend.domain.todo.service;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.todo.converter.TodoConverter;
import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 할 일 서비스 (임시)
 * TODO: TodoQueryService, TodoCommandService로 분리 예정
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public TodoResDTO.TodoInfo createTodo(TodoReqDTO.CreateTodo reqDTO, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        TodoRecurrenceGroup todoRecurrenceGroup = null;

        // TODO: 반복 할 일 처리는 TodoCommandService에서 구현 예정

        // 할 일 생성
        Todo todo = TodoConverter.toTodo(reqDTO, member, todoRecurrenceGroup);
        todo = todoRepository.save(todo);

        return TodoConverter.toTodoInfo(todo);
    }
}
