package com.project.backend.domain.todo.service.query;

import com.project.backend.domain.briefing.dto.TodayOccurrenceResult;
import com.project.backend.domain.event.enums.ExceptionType;
import com.project.backend.domain.event.factory.EndConditionFactory;
import com.project.backend.domain.event.factory.GeneratorFactory;
import com.project.backend.domain.event.strategy.endcondition.EndCondition;
import com.project.backend.domain.event.strategy.generator.Generator;
import com.project.backend.domain.reminder.enums.TargetType;
import com.project.backend.domain.todo.converter.TodoConverter;
import com.project.backend.domain.todo.dto.response.TodoResDTO;
import com.project.backend.domain.todo.entity.Todo;
import com.project.backend.domain.todo.entity.TodoRecurrenceException;
import com.project.backend.domain.todo.entity.TodoRecurrenceGroup;
import com.project.backend.domain.todo.enums.Priority;
import com.project.backend.domain.todo.enums.TodoFilter;
import com.project.backend.domain.todo.exception.TodoErrorCode;
import com.project.backend.domain.todo.exception.TodoException;
import com.project.backend.domain.todo.repository.TodoRecurrenceExceptionRepository;
import com.project.backend.domain.todo.repository.TodoRepository;
import com.project.backend.domain.reminder.dto.NextOccurrenceResult;
import com.project.backend.domain.reminder.entity.Reminder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoQueryServiceImpl implements TodoQueryService {

    private final TodoRepository todoRepository;
    private final TodoRecurrenceExceptionRepository todoRecurrenceExceptionRepository;
    private final GeneratorFactory generatorFactory;
    private final EndConditionFactory endConditionFactory;

    @Override
    public TodoResDTO.TodoListRes getTodos(Long memberId, TodoFilter filter) {
        List<Todo> todos = todoRepository.findByMemberId(memberId);

        List<TodoResDTO.TodoListItem> todoListItems = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Todo todo : todos) {
            if (todo.isRecurring()) {
                // 반복 할 일: 다음 1개만 계산
                LocalDate nextOccurrence = getNextOccurrence(todo, today);
                if (nextOccurrence != null) {
                    Boolean isCompleted = getCompletedStatus(todo, nextOccurrence);
                    todoListItems.add(TodoConverter.toTodoListItem(todo, nextOccurrence, isCompleted));
                }
            } else {
                // 단일 할 일
                todoListItems.add(TodoConverter.toTodoListItem(todo, todo.getStartDate(), todo.getIsCompleted()));
            }
        }

        // 필터 적용
        todoListItems = applyFilter(todoListItems, filter, today);

        if (filter == TodoFilter.PRIORITY) {
            return TodoResDTO.TodoListRes.builder()
                    .todos(todoListItems)
                    .build();
        } else {
            // 날짜순 정렬
            todoListItems.sort(Comparator.comparing(TodoResDTO.TodoListItem::occurrenceDate));
        }

        return TodoResDTO.TodoListRes.builder()
                .todos(todoListItems)
                .build();
    }

    @Override
    public TodoResDTO.TodoListRes getTodosForCalendar(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<Todo> todos = todoRepository.findByMemberId(memberId);

        List<TodoResDTO.TodoListItem> expandedTodos = expandTodos(todos, startDate, endDate);

        // 날짜순 정렬
        expandedTodos.sort(Comparator.comparing(TodoResDTO.TodoListItem::occurrenceDate));

        return TodoResDTO.TodoListRes.builder()
                .todos(expandedTodos)
                .build();
    }

    @Override
    public TodoResDTO.TodoDetailRes getTodoDetail(Long memberId, Long todoId, LocalDate occurrenceDate) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));

        // 권한 확인
        if (!todo.getMember().getId().equals(memberId)) {
            throw new TodoException(TodoErrorCode.TODO_FORBIDDEN);
        }

        // 단일 할 일인 경우 occurrenceDate 무시하고 startDate 사용
        if (!todo.isRecurring()) {
            return TodoConverter.toTodoDetailRes(todo, todo.getStartDate());
        }

        // 반복 할 일인 경우 occurrenceDate 필수
        if (occurrenceDate == null) {
            throw new TodoException(TodoErrorCode.OCCURRENCE_DATE_REQUIRED);
        }

        // 유효한 반복 날짜인지 검증
        if (!isValidOccurrenceDate(todo, occurrenceDate)) {
            throw new TodoException(TodoErrorCode.TODO_NOT_FOUND);
        }

        // 예외 확인
        Optional<TodoRecurrenceException> exception = todoRecurrenceExceptionRepository
                .findByTodoRecurrenceGroupIdAndExceptionDate(
                        todo.getTodoRecurrenceGroup().getId(),
                        occurrenceDate
                );

        if (exception.isPresent()) {
            TodoRecurrenceException ex = exception.get();
            if (ex.getExceptionType() == ExceptionType.SKIP) {
                throw new TodoException(TodoErrorCode.TODO_NOT_FOUND);
            }
            return TodoConverter.toTodoDetailRes(todo, occurrenceDate, ex);
        }

        return TodoConverter.toTodoDetailRes(todo, occurrenceDate);
    }

    @Override
    public TodoResDTO.TodoProgressRes getProgress(Long memberId, LocalDate date) {
        // 해당 날짜의 할 일들 조회
        TodoResDTO.TodoListRes todosForDay = getTodosForCalendar(memberId, date, date);

        int totalCount = todosForDay.todos().size();
        int completedCount = (int) todosForDay.todos().stream()
                .filter(TodoResDTO.TodoListItem::isCompleted)
                .count();

        return TodoConverter.toTodoProgressRes(date, totalCount, completedCount);
    }

    @Override
    public boolean isValidOccurrenceDate(Long todoId, LocalDate occurrenceDate) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));

        if (!todo.isRecurring()) {
            return false;
        }

        return isValidOccurrenceDate(todo, occurrenceDate);
    }

    public NextOccurrenceResult calculateNextOccurrence(Long todoId, LocalDateTime occurrenceTime) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));

        // 반복 그룹이 있는 일정일 경우
        if (todo.getTodoRecurrenceGroup() == null) {
            return NextOccurrenceResult.none();
        }

        // 생성기에 최초로 들어갈 기준 시간
        LocalDateTime current = todo.getStartDate().atTime(todo.getDueTime());
        TodoRecurrenceGroup rg = todo.getTodoRecurrenceGroup();

        // 생성기 & 종료 조건 생성
        Generator generator = generatorFactory.getGenerator(todo.getTodoRecurrenceGroup());
        EndCondition endCondition = endConditionFactory.getEndCondition(todo.getTodoRecurrenceGroup());

        int count = 1;

        while (endCondition.shouldContinue(current, count, rg)) {

            current = generator.next(current, rg);

            // 일정 정보가 들어간 리마인더의 occurrenceTime보다 이후일경우 바로 해당 시간 반환
            if (current.isAfter(occurrenceTime)) {
                return NextOccurrenceResult.of(current);
            }

            count++;

            if (count > 20_000) {
                break; // 안전장치
            }
        }
        return NextOccurrenceResult.none();
    }

    @Override
    public List<TodayOccurrenceResult> calculateTodayOccurrence(List<Long> todoIds, LocalDate currentDate) {
        List<TodayOccurrenceResult> result = new ArrayList<>();

        for (Long id : todoIds) {
            Todo todo = todoRepository.findById(id)
                    .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));

            // 1) 단일 할일
            if (!todo.isRecurring()) {
                if (todo.getStartDate().isEqual(currentDate)) {
                    LocalTime dueTime = todo.getDueTime() != null ? todo.getDueTime() : LocalTime.MIDNIGHT;
                    result.add(TodayOccurrenceResult.of(todo.getTitle(), dueTime, TargetType.TODO));
                } else {
                    result.add(TodayOccurrenceResult.none());
                }
                continue;
            }

            // 2) 반복 할일: "오늘을 포함한 가장 가까운 다음 occurrence"를 구하고, 그게 오늘이면 브리핑 대상임
            TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();

            Generator generator = generatorFactory.getGenerator(group);
            EndCondition endCondition = endConditionFactory.getEndCondition(group);

            // 예외 날짜 조회
            Set<LocalDate> skipDates = todoRecurrenceExceptionRepository
                    .findByTodoRecurrenceGroupId(group.getId())
                    .stream()
                    .filter(ex -> ex.getExceptionType() == ExceptionType.SKIP)
                    .map(TodoRecurrenceException::getExceptionDate)
                    .collect(Collectors.toSet());

            Optional<TodoRecurrenceException> re = todoRecurrenceExceptionRepository.
                    findByTodoRecurrenceGroupIdAndExceptionDate(group.getId(), currentDate)
                    .filter(ex -> ex.getExceptionType() == ExceptionType.OVERRIDE);

            // 기준 시간 설정 (startDate + dueTime)
            LocalTime dueTime = todo.getDueTime() != null ? todo.getDueTime() : LocalTime.MIDNIGHT;
            LocalDateTime current = todo.getStartDate().atTime(dueTime);

            String title = todo.getTitle();

            if (re.isPresent()) {
                TodoRecurrenceException exception = re.get();
                title = !Objects.equals(todo.getTitle(), exception.getTitle()) ? exception.getTitle() : todo.getTitle();
                if (exception.getDueTime() != null && !exception.getDueTime().equals(todo.getDueTime())) {
                    dueTime = exception.getDueTime();
                }
            }

            int count = 1;
            int maxIterations = 1000;

            // 첫 번째 날짜가 currentDate와 일치하는지
            if (current.toLocalDate().isEqual(currentDate) && !skipDates.contains(current.toLocalDate())) {
                result.add(TodayOccurrenceResult.of(title, dueTime, TargetType.TODO));
                continue;
            }

            while (endCondition.shouldContinue(current, count, group) && count < maxIterations) {
                current = generator.next(current, group);
                if (current == null) {
                    break;
                }

                // 계산된 일정의 날짜가 오늘보다 이후일 경우
                if (current.isAfter(currentDate.atTime(LocalTime.MAX))) {
                    result.add(TodayOccurrenceResult.none());
                    break;
                }

                // 계산된 일정 날짜가 오늘과 일치한다면
                if (current.toLocalDate().isEqual(currentDate)) {
                    result.add(TodayOccurrenceResult.of(title, dueTime, TargetType.TODO));
                    break;
                }

                count++;
            }
        }

        return result;
    }

    @Override
    public LocalDateTime findNextOccurrenceAfterNow(Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoException(TodoErrorCode.TODO_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        TodoRecurrenceGroup rg = todo.getTodoRecurrenceGroup();

        // 생성기 & 종료 조건 생성
        Generator generator = generatorFactory.getGenerator(todo.getTodoRecurrenceGroup());
        EndCondition endCondition = endConditionFactory.getEndCondition(todo.getTodoRecurrenceGroup());

        LocalDateTime current = todo.getStartDate().atTime(todo.getDueTime());
        LocalDateTime lastValid = null;

        int count = 1;

        while (endCondition.shouldContinue(current, count, rg)) {
            current = generator.next(current, rg);
            lastValid = current;
            count++;

            // 일정 정보가 들어간 리마인더의 occurrenceTime보다 이후일경우 바로 해당 시간 반환
            if (current.isAfter(now)) {
                return lastValid;
            }

            if (count > 20_000) {
                break; // 안전장치
            }
        }
        return lastValid;
    }

    // ===== Private Methods =====

    /**
     * 반복 할 일의 유효한 날짜인지 검증
     * - 시작일(startDate) 이후인지
     * - 종료일(endDate) 이전인지
     * - 반복 패턴에 맞는 날짜인지 (DAILY, WEEKLY, MONTHLY, YEARLY)
     */
    private boolean isValidOccurrenceDate(Todo todo, LocalDate occurrenceDate) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();
        if (group == null) {
            return false;
        }

        // 1. 시작일 이전이면 유효하지 않음
        if (occurrenceDate.isBefore(todo.getStartDate())) {
            return false;
        }

        // 2. 종료일 이후이면 유효하지 않음
        if (group.getEndDate() != null && occurrenceDate.isAfter(group.getEndDate())) {
            return false;
        }

        // 3. 시작일과 같으면 유효함
        if (occurrenceDate.equals(todo.getStartDate())) {
            return true;
        }

        // 4. 반복 패턴에 맞는 날짜인지 확인
        Generator generator = generatorFactory.getGenerator(group);
        EndCondition endCondition = endConditionFactory.getEndCondition(group);

        LocalTime dueTime = todo.getDueTime() != null ? todo.getDueTime() : LocalTime.MIDNIGHT;
        LocalDateTime current = todo.getStartDate().atTime(dueTime);

        int count = 1;
        int maxIterations = 10000; // 충분한 반복 횟수

        while (endCondition.shouldContinue(current, count, group) && count < maxIterations) {
            current = generator.next(current, group);
            if (current == null) {
                break;
            }

            LocalDate generatedDate = current.toLocalDate();

            // 종료일 체크
            if (group.getEndDate() != null && generatedDate.isAfter(group.getEndDate())) {
                break;
            }

            // 찾고자 하는 날짜와 일치하면 유효함
            if (generatedDate.equals(occurrenceDate)) {
                return true;
            }

            // 이미 지나쳤으면 더 이상 찾을 필요 없음
            if (generatedDate.isAfter(occurrenceDate)) {
                break;
            }

            count++;
        }

        return false;
    }

    /**
     * 오늘 이후 가장 가까운 다음 반복 날짜 계산
     */
    private LocalDate getNextOccurrence(Todo todo, LocalDate fromDate) {
        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();
        if (group == null) {
            return null;
        }

        Generator generator = generatorFactory.getGenerator(group);
        EndCondition endCondition = endConditionFactory.getEndCondition(group);

        // 예외 날짜 조회
        Set<LocalDate> skipDates = todoRecurrenceExceptionRepository
                .findByTodoRecurrenceGroupId(group.getId())
                .stream()
                .filter(ex -> ex.getExceptionType() == ExceptionType.SKIP)
                .map(TodoRecurrenceException::getExceptionDate)
                .collect(Collectors.toSet());

        // 기준 시간 설정 (startDate + dueTime)
        LocalTime dueTime = todo.getDueTime() != null ? todo.getDueTime() : LocalTime.MIDNIGHT;
        LocalDateTime current = todo.getStartDate().atTime(dueTime);
        LocalDateTime fromDateTime = fromDate.atStartOfDay();

        int count = 1;
        int maxIterations = 1000;

        // 첫 번째 날짜가 fromDate 이후인지 확인
        if (!current.toLocalDate().isBefore(fromDate) && !skipDates.contains(current.toLocalDate())) {
            return current.toLocalDate();
        }

        while (endCondition.shouldContinue(current, count, group) && count < maxIterations) {
            current = generator.next(current, group);
            if (current == null) {
                break;
            }

            LocalDate occurrenceDate = current.toLocalDate();

            // fromDate 이후이고 SKIP이 아닌 날짜면 반환
            if (!occurrenceDate.isBefore(fromDate) && !skipDates.contains(occurrenceDate)) {
                return occurrenceDate;
            }

            count++;
        }

        return null;
    }

    /**
     * 특정 날짜의 완료 상태 조회
     */
    private Boolean getCompletedStatus(Todo todo, LocalDate occurrenceDate) {
        if (!todo.isRecurring()) {
            return todo.getIsCompleted();
        }

        Optional<TodoRecurrenceException> exception = todoRecurrenceExceptionRepository
                .findByTodoRecurrenceGroupIdAndExceptionDate(
                        todo.getTodoRecurrenceGroup().getId(),
                        occurrenceDate
                );

        return exception.map(TodoRecurrenceException::getIsCompleted).orElse(false);
    }

    /**
     * 범위 내 할 일 펼치기
     */
    private List<TodoResDTO.TodoListItem> expandTodos(List<Todo> todos, LocalDate startDate, LocalDate endDate) {
        List<TodoResDTO.TodoListItem> expandedTodos = new ArrayList<>();

        for (Todo todo : todos) {
            if (todo.isRecurring()) {
                // 반복 할 일 펼치기
                expandedTodos.addAll(expandRecurringTodo(todo, startDate, endDate));
            } else {
                // 단일 할 일: 범위 내에 있으면 추가
                LocalDate todoStartDate = todo.getStartDate();
                if (!todoStartDate.isBefore(startDate) && !todoStartDate.isAfter(endDate)) {
                    expandedTodos.add(TodoConverter.toTodoListItem(todo, todoStartDate, todo.getIsCompleted()));
                }
            }
        }

        return expandedTodos;
    }

    /**
     * 반복 할 일을 범위 내에서 펼침
     */
    private List<TodoResDTO.TodoListItem> expandRecurringTodo(Todo todo, LocalDate startDate, LocalDate endDate) {
        List<TodoResDTO.TodoListItem> expanded = new ArrayList<>();

        TodoRecurrenceGroup group = todo.getTodoRecurrenceGroup();
        Generator generator = generatorFactory.getGenerator(group);
        EndCondition endCondition = endConditionFactory.getEndCondition(group);

        // 예외 조회
        Map<LocalDate, TodoRecurrenceException> exceptionMap = todoRecurrenceExceptionRepository
                .findByTodoRecurrenceGroupId(group.getId())
                .stream()
                .collect(Collectors.toMap(
                        TodoRecurrenceException::getExceptionDate,
                        ex -> ex,
                        (existing, replacement) -> existing
                ));

        // 기준 시간 설정
        LocalTime dueTime = todo.getDueTime() != null ? todo.getDueTime() : LocalTime.MIDNIGHT;
        LocalDateTime current = todo.getStartDate().atTime(dueTime);

        int count = 1;
        int maxIterations = 1000;

        // 첫 번째 날짜 처리
        LocalDate firstDate = current.toLocalDate();
        if (!firstDate.isBefore(startDate) && !firstDate.isAfter(endDate)) {
            TodoRecurrenceException exception = exceptionMap.get(firstDate);
            if (exception == null || exception.getExceptionType() != ExceptionType.SKIP) {
                expanded.add(createTodoListItem(todo, firstDate, exception));
            }
        }

        while (endCondition.shouldContinue(current, count, group) && count < maxIterations) {
            current = generator.next(current, group);
            if (current == null) {
                break;
            }

            LocalDate occurrenceDate = current.toLocalDate();

            // 범위 이전이면 스킵
            if (occurrenceDate.isBefore(startDate)) {
                count++;
                continue;
            }

            // 범위 이후면 종료
            if (occurrenceDate.isAfter(endDate)) {
                break;
            }

            // 종료 날짜 확인
            if (group.getEndDate() != null && occurrenceDate.isAfter(group.getEndDate())) {
                break;
            }

            // 예외 처리
            TodoRecurrenceException exception = exceptionMap.get(occurrenceDate);
            if (exception != null && exception.getExceptionType() == ExceptionType.SKIP) {
                count++;
                continue;
            }

            expanded.add(createTodoListItem(todo, occurrenceDate, exception));
            count++;
        }

        return expanded;
    }

    /**
     * TodoListItem 생성 (예외 적용)
     */
    private TodoResDTO.TodoListItem createTodoListItem(Todo todo, LocalDate occurrenceDate,
                                                        TodoRecurrenceException exception) {
        if (exception != null) {
            return TodoConverter.toTodoListItem(todo, occurrenceDate, exception);
        }
        return TodoConverter.toTodoListItem(todo, occurrenceDate, false);
    }

    /**
     * 필터 적용
     */
    private List<TodoResDTO.TodoListItem> applyFilter(List<TodoResDTO.TodoListItem> todos,
                                                       TodoFilter filter, LocalDate today) {
        return switch (filter) {
            case ALL -> todos;
            case TODAY -> todos.stream()
                    .filter(t -> t.occurrenceDate().equals(today))
                    .collect(Collectors.toList());
            case PRIORITY -> todos.stream()
                    .sorted(Comparator.comparing(TodoResDTO.TodoListItem::priority,
                            Comparator.comparingInt(p -> p == Priority.HIGH ? 0 : p == Priority.MEDIUM ? 1 : 2)))
                    .collect(Collectors.toList());
            case COMPLETED -> todos.stream()
                    .filter(TodoResDTO.TodoListItem::isCompleted)
                    .collect(Collectors.toList());
        };
    }
}
