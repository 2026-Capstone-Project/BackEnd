package com.project.backend.domain.todo.service.command;

import com.project.backend.domain.common.reminder.bridge.ReminderEventBridge;
import com.project.backend.domain.event.enums.ExceptionType;
import com.project.backend.domain.member.entity.Member;
import com.project.backend.domain.member.exception.MemberErrorCode;
import com.project.backend.domain.member.exception.MemberException;
import com.project.backend.domain.member.repository.MemberRepository;
import com.project.backend.domain.reminder.enums.ChangeType;
import com.project.backend.domain.reminder.enums.DeletedType;
import com.project.backend.domain.reminder.enums.ExceptionChangeType;
import com.project.backend.domain.reminder.enums.TargetType;
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
import java.time.LocalDateTime;
import java.util.Optional;

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
    private final ReminderEventBridge reminderEventBridge;

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

        // 투두 생성에 따른 리스너 생성 로직 실행
        reminderEventBridge.handlePlanChanged(
                todo.getId(),
                TargetType.TODO,
                memberId,
                todo.getTitle(),
                recurrenceGroup != null,
                todo.getDueTime() != null ? todo.getStartDate().atTime(todo.getDueTime()) : todo.getStartDate().atStartOfDay(),
                ChangeType.CREATED
        );
        return TodoConverter.toTodoInfo(todo);
    }

    @Override
    public TodoResDTO.TodoInfo updateTodo(Long memberId, Long todoId, LocalDate occurrenceDate,
                                           RecurrenceUpdateScope scope, TodoReqDTO.UpdateTodo reqDTO) {
        Todo todo = getTodoWithPermissionCheck(memberId, todoId);

        // 단일 할 일인 경우
        if (!todo.isRecurring()) {
            updateSingleTodo(todo, reqDTO);
            // 이벤트 생성에 따른 리스너 생성 로직 실행
            reminderEventBridge.handlePlanChanged(
                    todo.getId(),
                    TargetType.TODO,
                    memberId,
                    todo.getTitle(),
                    false,
                    todo.getDueTime() != null ? todo.getStartDate().atTime(todo.getDueTime()) : todo.getStartDate().atStartOfDay(),
                    ChangeType.UPDATE_SINGLE
            );
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
            case THIS_TODO -> updateThisTodoOnly(todo, occurrenceDate, reqDTO, memberId);
            case THIS_AND_FOLLOWING -> updateThisAndFollowing(todo, occurrenceDate, reqDTO);
        };
    }

    @Override
    public void deleteTodo(Long memberId, Long todoId, LocalDate occurrenceDate, RecurrenceUpdateScope scope) {
        Todo todo = getTodoWithPermissionCheck(memberId, todoId);

        // 단일 할 일인 경우
        if (!todo.isRecurring()) {
            todoRepository.delete(todo);
            log.debug("단일 할 일 삭제 완료 - todoId: {}", todoId);
            reminderEventBridge.handleReminderDeleted(
                    null,
                    memberId,
                    todo.getDueTime() != null ? occurrenceDate.atTime(todo.getDueTime()) : occurrenceDate.atStartOfDay(),
                    todoId,
                    TargetType.TODO,
                    DeletedType.DELETED_SINGLE);
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
            case THIS_TODO -> deleteThisTodoOnly(todo, occurrenceDate, memberId);
            case THIS_AND_FOLLOWING -> deleteThisAndFollowing(todo, occurrenceDate, memberId);
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
                                                    TodoReqDTO.UpdateTodo reqDTO, Long memberId) {
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

            // Exception을 가진 할 일을 재 수정 했을 때 리마인더 처리 실행
            reminderEventBridge.handleExceptionChanged(
                    exception.getId(),
                    todo.getId(),
                    TargetType.TODO,
                    memberId,
                    exception.getTitle(),
                    exception.getExceptionDate().atTime(exception.getDueTime()),
                    ExceptionChangeType.UPDATE_THIS_AGAIN
            );
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

            // 원본 할 일 수정 시 리마인더 처리
            reminderEventBridge.handleExceptionChanged(
                    exception.getId(),
                    todo.getId(),
                    TargetType.TODO,
                    memberId,
                    exception.getTitle(),
                    exception.getExceptionDate().atTime(exception.getDueTime()),
                    ExceptionChangeType.UPDATED_THIS);
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

        LocalDateTime startDate = occurrenceDate.atTime(
                reqDTO.dueTime() != null ? reqDTO.dueTime() : todo.getDueTime()
        );

        // dueTime 변경하지 않은 경우
        if (reqDTO.dueTime() == null) {
            Optional<TodoRecurrenceException> ex = todoRecurrenceExceptionRepository
                    .findByTodoRecurrenceGroupIdAndExceptionDate(oldGroup.getId(), occurrenceDate);

            // 수정된 할 일에 대한 수정인 경우 수정된 dueTime으로 설정
            if (ex.isPresent()) {
                TodoRecurrenceException re = ex.get();
                if (re.getExceptionType() == ExceptionType.SKIP) {
                    throw new TodoException(TodoErrorCode.TODO_NOT_FOUND);
                }

                if (re.getExceptionType() == ExceptionType.OVERRIDE
                        && !re.getDueTime().equals(startDate.toLocalTime())) {
                    startDate = occurrenceDate.atTime(re.getDueTime());
                }
            }
        }
        // 새 Todo 생성
        Todo newTodo = Todo.createRecurring(
                member,
                reqDTO.title() != null ? reqDTO.title() : todo.getTitle(),
                reqDTO.startDate() != null ? reqDTO.startDate() : occurrenceDate,
                startDate.toLocalTime(),
                reqDTO.isAllDay() != null ? reqDTO.isAllDay() : todo.getIsAllDay(),
                reqDTO.priority() != null ? reqDTO.priority() : todo.getPriority(),
                reqDTO.color() != null ? reqDTO.color() : todo.getColor(),
                reqDTO.memo() != null ? reqDTO.memo() : todo.getMemo(),
                newGroup
        );

        newTodo = todoRepository.save(newTodo);

        // 반복 그룹에 Todo 연결
        newGroup.setTodo(newTodo);

        // 원본 할 일에 대한 수정일 경우 기존 할 일 + 반복 삭제
        if (todo.getStartDate().equals(occurrenceDate)) {
            todoRepository.delete(todo);
            todoRecurrenceGroupRepository.delete(oldGroup);
            reminderEventBridge.handleReminderDeleted(
                    null,
                    member.getId(),
                    todo.getStartDate().atTime(todo.getDueTime()),
                    todo.getId(),
                    TargetType.TODO,
                    DeletedType.DELETED_ALL);
        } else {
            // 해당 일정과 그 이후 일정들을 수정했을 때 리스너 수정 로직 실행
            // 기존 일정에 대한 리마인더 삭제 여부 결정
            reminderEventBridge.handleReminderDeleted(
                    null,
                    member.getId(),
                    startDate,
                    todo.getId(),
                    TargetType.TODO,
                    DeletedType.DELETED_THIS_AND_FOLLOWING
            );
        }

        // 수정하려는 날짜 포함한 이후 할 일들에 대한 반복예외 객체 모두 삭제
        todoRecurrenceExceptionRepository.deleteByTodoRecurrenceGroupIdAndOccurrenceDate(
                oldGroup.getId(), occurrenceDate
        );

        // 새 일정 생성에 대한 리마인더 발생
        reminderEventBridge.handlePlanChanged(
                newTodo.getId(),
                TargetType.TODO,
                member.getId(),
                newTodo.getTitle(),
                true,
                newTodo.getStartDate().atTime(newTodo.getDueTime()),
                ChangeType.CREATED
        );

        log.debug("반복 할 일 이후 수정 완료 - oldTodoId: {}, newTodoId: {}", todo.getId(), newTodo.getId());
        return TodoConverter.toTodoInfo(newTodo);
    }

    /**
     * 반복 할 일 - 모든 할 일 수정
     */
//     private TodoResDTO.TodoInfo updateAllTodos(Todo todo, TodoReqDTO.UpdateTodo reqDTO) {
//         // 원본 Todo 수정
//         todo.update(
//                 reqDTO.title(),
//                 reqDTO.startDate(),
//                 reqDTO.dueTime(),
//                 reqDTO.isAllDay(),
//                 reqDTO.priority(),
//                 reqDTO.color(),
//                 reqDTO.memo()
//         );

//         // 반복 종료일 변경
//         if (reqDTO.endDate() != null && todo.getTodoRecurrenceGroup() != null) {
//             todo.getTodoRecurrenceGroup().updateEndByDate(reqDTO.endDate());
//         }

//         log.debug("반복 할 일 전체 수정 완료 - todoId: {}", todo.getId());

//         return TodoConverter.toTodoInfo(todo);
//     }

    /**
     * 반복 할 일 - 이 할 일만 삭제 (SKIP 예외 생성)
     */
    private void deleteThisTodoOnly(Todo todo, LocalDate occurrenceDate, Long memberId) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();

        LocalDateTime startTime = todo.getDueTime() != null ? occurrenceDate.atTime(todo.getDueTime()) : occurrenceDate.atStartOfDay();

        Optional<TodoRecurrenceException> re = todoRecurrenceExceptionRepository
                .findByTodoRecurrenceGroupIdAndExceptionDate(group.getId(), occurrenceDate);

        if (re.isPresent()) {
            TodoRecurrenceException ex = re.get();

            // 삭제된 할 일인 경우
            if (ex.getExceptionType() == ExceptionType.SKIP) {
                throw new TodoException(TodoErrorCode.TODO_NOT_FOUND);
            }

            // 기존 예외가 있으면 SKIP으로 상태 변경
            ex.updateExceptionTypeToSKIP();
            startTime = todo.getStartDate().atTime(ex.getDueTime());

            if (startTime.isAfter(LocalDateTime.now())) {
                reminderEventBridge.handleReminderDeleted(
                        ex.getId(),
                        memberId,
                        startTime,
                        todo.getId(),
                        TargetType.TODO,
                        DeletedType.DELETED_SINGLE
                );
            }
            return;
        }

        // SKIP 예외 생성
        TodoRecurrenceException exception = TodoRecurrenceException.createSkip(group, occurrenceDate);
        todoRecurrenceExceptionRepository.save(exception);
        group.addExceptionDate(exception);

        // 해당 일정만 삭제했을 때 리스너 수정 로직 실행
        reminderEventBridge.handleExceptionChanged(
                exception.getId(),
                todo.getId(),
                TargetType.TODO,
                memberId,
                todo.getTitle(),
                startTime,
                ExceptionChangeType.DELETED_THIS);

        log.debug("반복 할 일 예외 삭제 완료 - todoId: {}, date: {}", todo.getId(), occurrenceDate);
    }

    /**
     * 반복 할 일 - 이 할 일 및 이후 삭제 (종료 날짜 변경)
     */
    private void deleteThisAndFollowing(Todo todo, LocalDate occurrenceDate, Long memberId) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();

        // 삭제하려는 날짜가 수정된 일정인지
        Optional<TodoRecurrenceException> re = todoRecurrenceExceptionRepository.
                findByTodoRecurrenceGroupIdAndExceptionDate(group.getId(), occurrenceDate);

        LocalDateTime startDate = todo.getDueTime() != null ? occurrenceDate.atTime(todo.getDueTime()) : occurrenceDate.atStartOfDay();

        // 수정/삭제된 할 일일때
        if (re.isPresent()) {
            TodoRecurrenceException ex = re.get();

            // 삭제된 할 일이면
            if (ex.getExceptionType() == ExceptionType.SKIP) {
                throw new TodoException(TodoErrorCode.TODO_NOT_FOUND);
            }

            // dueTime이 수정된 할 일인 경우
            if (todo.getDueTime() != ex.getDueTime()) {
                startDate = occurrenceDate.atTime(ex.getDueTime());
            }
        }

        // 삭제하려는 날을 포함한 이후 할 일들에 대한 반복예외 객체 모두 삭제
        todoRecurrenceExceptionRepository.deleteByTodoRecurrenceGroupIdAndOccurrenceDate(group.getId(), occurrenceDate);

        // 원본 할 일에 대한 수정이면 기존 할 일 + 반복 삭제
        if (todo.getStartDate().equals(occurrenceDate)) {
            todoRepository.delete(todo);
            todoRecurrenceGroupRepository.delete(todo.getTodoRecurrenceGroup());
            reminderEventBridge.handleReminderDeleted(
                    null,
                    memberId,
                    startDate,
                    todo.getId(),
                    TargetType.TODO,
                    DeletedType.DELETED_ALL
            );
        } else {
            // 반복 그룹의 종료 날짜를 해당 날짜 전날로 설정
            group.updateEndByDate(occurrenceDate.minusDays(1));

            // 해당 일정과 그 이후 일정들을 삭제했을 때 리스너 수정 로직 실행
            reminderEventBridge.handleReminderDeleted(
                    null,
                    memberId,
                    startDate,
                    todo.getId(),
                    TargetType.TODO,
                    DeletedType.DELETED_THIS_AND_FOLLOWING
            );
        }

        log.debug("반복 할 일 이후 삭제 완료 - todoId: {}, newEndDate: {}", todo.getId(), occurrenceDate.minusDays(1));
    }

    /**
     * 반복 할 일 - 모든 할 일 삭제
     */
//    private void deleteAllTodos(Todo todo) {
//        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();
//
//        // Todo 삭제 (cascade로 예외도 삭제됨)
//        todoRepository.delete(todo);
//
//        // 반복 그룹 삭제
//        todoRecurrenceGroupRepository.delete(group);
//
//        log.debug("반복 할 일 전체 삭제 완료 - todoId: {}, groupId: {}", todo.getId(), group.getId());
//    }

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
