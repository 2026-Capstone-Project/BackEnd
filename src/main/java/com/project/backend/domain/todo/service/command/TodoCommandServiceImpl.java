package com.project.backend.domain.todo.service.command;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.todo.converter.TodoConverter;
import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import com.project.backend.domain.todo.repository.TodoRecurrenceGroupRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TodoCommandServiceImpl implements TodoCommandService {

    private final MemberRepository memberRepository;
    private final TodoRepository todoRepository;
    private final TodoRecurrenceGroupRepository todoRecurrenceGroupRepository;

    @Override
    public TodoResDTO.TodoInfo createTodo(Long memberId, TodoReqDTO.CreateTodo reqDTO) {
        // 1. Member 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 2. 반복 그룹 생성 (있는 경우)
        TodoRecurrenceGroup recurrenceGroup = null;
        if (reqDTO.recurrenceGroup() != null) {
            recurrenceGroup = TodoConverter.toTodoRecurrenceGroup(reqDTO.recurrenceGroup(), member);
            recurrenceGroup = todoRecurrenceGroupRepository.save(recurrenceGroup);
            log.info("반복 할 일 그룹 생성 완료 - groupId: {}", recurrenceGroup.getId());
        }

        // 3. Todo 생성
        Todo todo = TodoConverter.toTodo(reqDTO, member, recurrenceGroup);
        todo = todoRepository.save(todo);
        log.info("할 일 생성 완료 - todoId: {}", todo.getId());

        // 4. 반복 그룹에 Todo 연결
        if (recurrenceGroup != null) {
            recurrenceGroup.setTodo(todo);
        }

        return TodoConverter.toTodoInfo(todo);
    }
}
