package com.project.backend.domain.todo.service.command;

import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.todo.converter.TodoConverter;
import com.project.backend.domain.todo.dto.request.TodoReqDTO;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceException;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import com.project.backend.domain.todo.enums.RecurrenceUpdateScope;
import com.project.backend.domain.todo.exception.TodoErrorCode;
import com.project.backend.domain.todo.exception.TodoException;
import com.project.backend.domain.todo.repository.TodoRecurrenceExceptionRepository;
import com.project.backend.domain.todo.repository.TodoRecurrenceGroupRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import com.project.backend.domain.todo.service.query.TodoQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TodoCommandServiceImpl implements TodoCommandService {

    private final MemberRepository memberRepository;
    private final TodoRepository todoRepository;
    private final TodoRecurrenceGroupRepository todoRecurrenceGroupRepository;
    private final TodoRecurrenceExceptionRepository todoRecurrenceExceptionRepository;
    private final TodoQueryService todoQueryService;

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
            log.debug("반복 할 일 그룹 생성 완료 - groupId: {}", recurrenceGroup.getId());
        }

        // 3. Todo 생성
        Todo todo = TodoConverter.toTodo(reqDTO, member, recurrenceGroup);
        todo = todoRepository.save(todo);
        log.debug("할 일 생성 완료 - todoId: {}", todo.getId());

        // 4. 반복 그룹에 Todo 연결
        if (recurrenceGroup != null) {
            recurrenceGroup.setTodo(todo);
        }

        return TodoConverter.toTodoInfo(todo);
    }

    @Override
    public TodoResDTO.TodoInfo updateTodo(Long memberId, Long todoId, LocalDate occurrenceDate,
                                           RecurrenceUpdateScope scope, TodoReqDTO.UpdateTodo reqDTO) {
        Todo todo = getTodoWithPermissionCheck(memberId, todoId);

        // 단일 할 일인 경우
        if (!todo.isRecurring()) {
            updateSingleTodo(todo, reqDTO);
            return TodoConverter.toTodoInfo(todo);
        }

        // 반복 할 일인 경우 occurrenceDate 필수
        if (occurrenceDate == null) {
            throw new TodoException(TodoErrorCode.OCCURRENCE_DATE_REQUIRED);
        }

        // scope 필수
        if (scope == null) {
            throw new TodoException(TodoErrorCode.INVALID_UPDATE_SCOPE);
        }

        // 유효한 반복 날짜인지 검증
        if (!todoQueryService.isValidOccurrenceDate(todoId, occurrenceDate)) {
            throw new TodoException(TodoErrorCode.TODO_NOT_FOUND);
        }

        return switch (scope) {
            case THIS_TODO -> updateThisTodoOnly(todo, occurrenceDate, reqDTO);
            case THIS_AND_FOLLOWING -> updateThisAndFollowing(todo, occurrenceDate, reqDTO);
            case ALL_TODOS -> updateAllTodos(todo, reqDTO);
        };
    }

    @Override
    public void deleteTodo(Long memberId, Long todoId, LocalDate occurrenceDate, RecurrenceUpdateScope scope) {
        Todo todo = getTodoWithPermissionCheck(memberId, todoId);

        // 단일 할 일인 경우
        if (!todo.isRecurring()) {
            todoRepository.delete(todo);
            log.debug("단일 할 일 삭제 완료 - todoId: {}", todoId);
            return;
        }

        // 반복 할 일인 경우 occurrenceDate 필수
        if (occurrenceDate == null) {
            throw new TodoException(TodoErrorCode.OCCURRENCE_DATE_REQUIRED);
        }

        // scope 필수
        if (scope == null) {
            throw new TodoException(TodoErrorCode.INVALID_UPDATE_SCOPE);
        }

        // 유효한 반복 날짜인지 검증
        if (!todoQueryService.isValidOccurrenceDate(todoId, occurrenceDate)) {
            throw new TodoException(TodoErrorCode.TODO_NOT_FOUND);
        }

        switch (scope) {
            case THIS_TODO -> deleteThisTodoOnly(todo, occurrenceDate);
            case THIS_AND_FOLLOWING -> deleteThisAndFollowing(todo, occurrenceDate);
            case ALL_TODOS -> deleteAllTodos(todo);
        }
    }

    @Override
    public TodoResDTO.TodoCompleteRes updateCompleteStatus(Long memberId, Long todoId,
                                                            LocalDate occurrenceDate, boolean isCompleted) {
        Todo todo = getTodoWithPermissionCheck(memberId, todoId);

        // 단일 할 일인 경우
        if (!todo.isRecurring()) {
            if (isCompleted) {
                todo.complete();
            } else {
                todo.incomplete();
            }
            log.debug("단일 할 일 완료 상태 변경 - todoId: {}, isCompleted: {}", todoId, isCompleted);
            return TodoConverter.toTodoCompleteRes(todo, todo.getStartDate(), isCompleted);
        }

        // 반복 할 일인 경우 occurrenceDate 필수
        if (occurrenceDate == null) {
            throw new TodoException(TodoErrorCode.OCCURRENCE_DATE_REQUIRED);
        }

        // 유효한 반복 날짜인지 검증
        if (!todoQueryService.isValidOccurrenceDate(todoId, occurrenceDate)) {
            throw new TodoException(TodoErrorCode.TODO_NOT_FOUND);
        }

        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();

        // 해당 날짜의 예외 조회
        TodoRecurrenceException exception = todoRecurrenceExceptionRepository
                .findByTodoRecurrenceGroupIdAndExceptionDate(group.getId(), occurrenceDate)
                .orElse(null);

        if (exception != null) {
            // 기존 예외가 있으면 완료 상태 변경
            if (isCompleted) {
                exception.complete();
            } else {
                exception.incomplete();
            }
            log.debug("반복 할 일 완료 상태 변경 (기존 예외 수정) - todoId: {}, date: {}, isCompleted: {}",
                    todoId, occurrenceDate, isCompleted);
        } else if (isCompleted) {
            // 완료로 변경할 때만 새 예외 생성 (미완료는 기본값이므로 생성 불필요)
            TodoRecurrenceException newException = TodoRecurrenceException.createCompleted(group, occurrenceDate);
            todoRecurrenceExceptionRepository.save(newException);
            group.addExceptionDate(newException);
            log.debug("반복 할 일 완료 상태 변경 (새 예외 생성) - todoId: {}, date: {}", todoId, occurrenceDate);
        }

        return TodoConverter.toTodoCompleteRes(todo, occurrenceDate, isCompleted);
    }

    // ===== Private Methods =====

    /**
     * 권한 체크와 함께 Todo 조회
     */
    private Todo getTodoWithPermissionCheck(Long memberId, Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));

        if (!todo.getMember().getId().equals(memberId)) {
            throw new TodoException(TodoErrorCode.TODO_FORBIDDEN);
        }

        return todo;
    }

    /**
     * 단일 할 일 수정
     */
    private void updateSingleTodo(Todo todo, TodoReqDTO.UpdateTodo reqDTO) {
        todo.update(
                reqDTO.title(),
                reqDTO.startDate(),
                reqDTO.dueTime(),
                reqDTO.isAllDay(),
                reqDTO.priority(),
                reqDTO.color(),
                reqDTO.memo()
        );
        log.debug("단일 할 일 수정 완료 - todoId: {}", todo.getId());
    }

    /**
     * 반복 할 일 - 이 할 일만 수정 (OVERRIDE 예외 생성/수정)
     */
    private TodoResDTO.TodoInfo updateThisTodoOnly(Todo todo, LocalDate occurrenceDate,
                                                    TodoReqDTO.UpdateTodo reqDTO) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();

        // 기존 예외 조회
        TodoRecurrenceException existingException = todoRecurrenceExceptionRepository
                .findByTodoRecurrenceGroupIdAndExceptionDate(group.getId(), occurrenceDate)
                .orElse(null);

        TodoRecurrenceException exception;

        if (existingException != null) {
            // 기존 예외가 있으면 업데이트
            String newTitle = reqDTO.title() != null ? reqDTO.title()
                    : (existingException.getTitle() != null ? existingException.getTitle() : todo.getTitle());
            LocalDate newStartDate = reqDTO.startDate() != null ? reqDTO.startDate()
                    : (existingException.getStartDate() != null ? existingException.getStartDate() : null);
            java.time.LocalTime newDueTime = reqDTO.dueTime() != null ? reqDTO.dueTime()
                    : (existingException.getDueTime() != null ? existingException.getDueTime() : todo.getDueTime());
            com.project.backend.domain.todo.enums.Priority newPriority = reqDTO.priority() != null ? reqDTO.priority()
                    : (existingException.getPriority() != null ? existingException.getPriority() : todo.getPriority());
            com.project.backend.domain.todo.enums.TodoColor newColor = reqDTO.color() != null ? reqDTO.color()
                    : (existingException.getColor() != null ? existingException.getColor() : todo.getColor());
            String newMemo = reqDTO.memo() != null ? reqDTO.memo()
                    : (existingException.getMemo() != null ? existingException.getMemo() : todo.getMemo());

            existingException.updateOverride(newTitle, newStartDate, newDueTime, newPriority, newColor, newMemo);
            exception = existingException;
            log.debug("반복 할 일 예외 수정 완료 (업데이트) - todoId: {}, date: {}", todo.getId(), occurrenceDate);
        } else {
            // 기존 예외가 없으면 새로 생성
            exception = TodoRecurrenceException.createOverride(
                    group,
                    occurrenceDate,
                    reqDTO.title() != null ? reqDTO.title() : todo.getTitle(),
                    reqDTO.startDate(),
                    reqDTO.dueTime() != null ? reqDTO.dueTime() : todo.getDueTime(),
                    reqDTO.priority() != null ? reqDTO.priority() : todo.getPriority(),
                    reqDTO.color() != null ? reqDTO.color() : todo.getColor(),
                    reqDTO.memo() != null ? reqDTO.memo() : todo.getMemo()
            );
            todoRecurrenceExceptionRepository.save(exception);
            group.addExceptionDate(exception);
            log.debug("반복 할 일 예외 수정 완료 (생성) - todoId: {}, date: {}", todo.getId(), occurrenceDate);
        }

        // 수정된 예외 정보를 포함하여 반환
        return TodoConverter.toTodoInfo(todo, occurrenceDate, exception);
    }

    /**
     * 반복 할 일 - 이 할 일 및 이후 수정 (기존 종료 + 새 생성)
     */
    private TodoResDTO.TodoInfo updateThisAndFollowing(Todo todo, LocalDate occurrenceDate,
                                                        TodoReqDTO.UpdateTodo reqDTO) {
        TodoRecurrenceGroup oldGroup = todo.getTodoRecurrenceGroup();
        Member member = todo.getMember();

        // 새 반복 그룹 생성 (요청에 없으면 기존 설정 복사) - oldGroup 수정 전에 먼저 복사!
        TodoRecurrenceGroup newGroup;
        if (reqDTO.recurrenceGroup() != null) {
            newGroup = TodoConverter.toTodoRecurrenceGroup(reqDTO.recurrenceGroup(), member);
        } else {
            // 기존 반복 설정 복사 (원본 endDate 유지)
            newGroup = copyRecurrenceGroup(oldGroup, member);
        }
        newGroup = todoRecurrenceGroupRepository.save(newGroup);

        // endDate가 별도로 제공된 경우 새 그룹의 종료일 변경
        if (reqDTO.endDate() != null) {
            newGroup.updateEndByDate(reqDTO.endDate());
        }

        // 기존 반복 그룹의 종료 날짜를 해당 날짜 전날로 설정 (복사 후에 수정!)
        oldGroup.updateEndByDate(occurrenceDate.minusDays(1));

        // 새 Todo 생성
        Todo newTodo = Todo.createRecurring(
                member,
                reqDTO.title() != null ? reqDTO.title() : todo.getTitle(),
                occurrenceDate,
                reqDTO.dueTime() != null ? reqDTO.dueTime() : todo.getDueTime(),
                reqDTO.isAllDay() != null ? reqDTO.isAllDay() : todo.getIsAllDay(),
                reqDTO.priority() != null ? reqDTO.priority() : todo.getPriority(),
                reqDTO.color() != null ? reqDTO.color() : todo.getColor(),
                reqDTO.memo() != null ? reqDTO.memo() : todo.getMemo(),
                newGroup
        );

        newTodo = todoRepository.save(newTodo);

        // 반복 그룹에 Todo 연결
        newGroup.setTodo(newTodo);

        log.debug("반복 할 일 이후 수정 완료 - oldTodoId: {}, newTodoId: {}", todo.getId(), newTodo.getId());

        return TodoConverter.toTodoInfo(newTodo);
    }

    /**
     * 반복 할 일 - 모든 할 일 수정
     */
    private TodoResDTO.TodoInfo updateAllTodos(Todo todo, TodoReqDTO.UpdateTodo reqDTO) {
        // 원본 Todo 수정
        todo.update(
                reqDTO.title(),
                reqDTO.startDate(),
                reqDTO.dueTime(),
                reqDTO.isAllDay(),
                reqDTO.priority(),
                reqDTO.color(),
                reqDTO.memo()
        );

        // 반복 종료일 변경
        if (reqDTO.endDate() != null && todo.getTodoRecurrenceGroup() != null) {
            todo.getTodoRecurrenceGroup().updateEndByDate(reqDTO.endDate());
        }

        log.debug("반복 할 일 전체 수정 완료 - todoId: {}", todo.getId());

        return TodoConverter.toTodoInfo(todo);
    }

    /**
     * 반복 할 일 - 이 할 일만 삭제 (SKIP 예외 생성)
     */
    private void deleteThisTodoOnly(Todo todo, LocalDate occurrenceDate) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();

        // 기존 예외가 있으면 삭제
        todoRecurrenceExceptionRepository
                .findByTodoRecurrenceGroupIdAndExceptionDate(group.getId(), occurrenceDate)
                .ifPresent(todoRecurrenceExceptionRepository::delete);

        // SKIP 예외 생성
        TodoRecurrenceException exception = TodoRecurrenceException.createSkip(group, occurrenceDate);
        todoRecurrenceExceptionRepository.save(exception);
        group.addExceptionDate(exception);

        log.debug("반복 할 일 예외 삭제 완료 - todoId: {}, date: {}", todo.getId(), occurrenceDate);
    }

    /**
     * 반복 할 일 - 이 할 일 및 이후 삭제 (종료 날짜 변경)
     */
    private void deleteThisAndFollowing(Todo todo, LocalDate occurrenceDate) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();

        // 반복 그룹의 종료 날짜를 해당 날짜 전날로 설정
        group.updateEndByDate(occurrenceDate.minusDays(1));

        log.debug("반복 할 일 이후 삭제 완료 - todoId: {}, newEndDate: {}", todo.getId(), occurrenceDate.minusDays(1));
    }

    /**
     * 반복 할 일 - 모든 할 일 삭제
     */
    private void deleteAllTodos(Todo todo) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();

        // Todo 삭제 (cascade로 예외도 삭제됨)
        todoRepository.delete(todo);

        // 반복 그룹 삭제
        todoRecurrenceGroupRepository.delete(group);

        log.debug("반복 할 일 전체 삭제 완료 - todoId: {}, groupId: {}", todo.getId(), group.getId());
    }

    /**
     * 기존 반복 그룹 설정을 복사하여 새 그룹 생성
     */
    private TodoRecurrenceGroup copyRecurrenceGroup(TodoRecurrenceGroup oldGroup, Member member) {
        return TodoRecurrenceGroup.create(
                member,
                oldGroup.getFrequency(),
                oldGroup.getIntervalValue(),
                oldGroup.getDaysOfWeek(),
                oldGroup.getMonthlyType(),
                oldGroup.getDaysOfMonth(),
                oldGroup.getWeekOfMonth(),
                oldGroup.getDayOfWeekInMonth(),
                oldGroup.getEndType(),
                oldGroup.getEndDate(),
                oldGroup.getOccurrenceCount()
        );
    }
}
