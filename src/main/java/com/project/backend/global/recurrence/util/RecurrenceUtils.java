package com.project.backend.global.recurrence.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 반복 로직에서 공용으로 사용하는 유틸리티 클래스
 */
// 유틸리티 클래스 인스턴스화 방지 -> static으로만 사용 가능하게 막는다는 뜻
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecurrenceUtils {

    /**
     * 정렬된 대상 목록에서 기준값 다음으로 오는 가장 가까운 값을 찾는다.
     * 기준값이 목록의 마지막 값이거나 그보다 크면 첫 번째 값을 반환한다 (순환).
     *
     * @param base 기준값
     * @param targets 대상 목록 (정렬되어 있지 않아도 됨)
     * @param <T> Comparable을 구현한 타입
     * @return 다음 타겟 값
     */
    // TODO : 반복 리팩토링
    // 반복 대상에서 기준 다음이면서 가장 작은 값을 찾는 메서드
    public static <T extends Comparable<T>> T findNextTarget(T base, List<T> targets) {
        // 가변 리스트로 복사 후 정렬
        List<T> sortedTargets = new ArrayList<>(targets);
        Collections.sort(sortedTargets);

        // 바이너리 서치
        int idx = Collections.binarySearch(sortedTargets, base);

        if (idx < 0) {
            // 정확한 값을 찾지 못한 경우: 삽입 위치의 인덱스
            idx = -idx - 1;
            // NPE 방지
            idx = idx % sortedTargets.size();

            return sortedTargets.get(idx);
        }

        // 정확히 찾은 경우: 그 다음 값을 반환 (순환)
        return sortedTargets.get((idx + 1) % sortedTargets.size());
    }

    /**
     * 콤마로 구분된 요일 문자열을 DayOfWeek 리스트로 변환한다.
     * 예: "MONDAY,WEDNESDAY,FRIDAY" → [MONDAY, WEDNESDAY, FRIDAY]
     *
     * @param daysOfWeek 콤마로 구분된 요일 문자열
     * @return DayOfWeek 리스트
     */
    public static List<DayOfWeek> parseDaysOfWeek(String daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(daysOfWeek.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 콤마로 구분된 일자 문자열을 Integer 리스트로 변환한다.
     * 예: "1,15,30" → [1, 15, 30]
     *
     * @param daysOfMonth 콤마로 구분된 일자 문자열
     * @return Integer 리스트
     */
    public static List<Integer> parseDaysOfMonth(String daysOfMonth) {
        if (daysOfMonth == null || daysOfMonth.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(daysOfMonth.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 콤마로 구분된 월 문자열을 Integer 리스트로 변환한다.
     * 예: "1,6,12" → [1, 6, 12]
     *
     * @param monthsOfYear 콤마로 구분된 월 문자열
     * @return Integer 리스트
     */
    public static List<Integer> parseMonthsOfYear(String monthsOfYear) {
        if (monthsOfYear == null || monthsOfYear.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(monthsOfYear.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * MONTHLY - DAY_OF_WEEK 규칙에 따라
     * 특정 월에서 "n번째 주차"에 해당하는 요일을 계산한다.

     * 주차 정의:
     *  - 해당 달의 "1일이 속한 주"를 1주차로 간주한다.
     *  - 주의 시작 요일은 ISO 기준(MONDAY)이다.

     * 동작 방식:
     *  1. 해당 달의 1일을 기준으로,
     *     그 날짜가 속한 주의 월요일을 1주차 시작일로 계산한다.
     *  2. weekOfMonth 값만큼 주차를 이동하여 대상 주의 시작일을 구한다.
     *  3. 해당 주(월~일) 범위 안에서,
     *     targetDays에 포함된 요일 중 "해당 월에 속한 날짜"만 선택한다.

     * 반환 규칙 (SKIP 정책):
     *  - 해당 주차에 targetDays가 존재하면 → 해당 날짜 반환
     *  - 존재하지 않으면 → Optional.empty()

     * 예시:
     *  - 2026년 4월, weekOfMonth=1, targetDays=[FRIDAY]
     *    → 2026-04-03 반환
     *  - 2026년 2월, weekOfMonth=5, targetDays=[FRIDAY]
     *    → Optional.empty() (5주차 자체가 없음)
     *
     * @param month       대상 연-월
     * @param ordinal 번째 (1부터 시작)
     * @param targetDays  허용된 요일 목록
     * @return 조건을 만족하는 날짜 (없으면 Optional.empty())
     */
    public static Optional<LocalDate> calculateMonthlyNthOrdinalWeekday(
            YearMonth month,
            int ordinal,                 // 1~5
            List<DayOfWeek> targetDays   // 허용 요일 집합(단일/주중/주말/전체/다중선택)
    ) {
        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal must be >= 1");
        }
        if (targetDays == null || targetDays.isEmpty()) {
            return Optional.empty();
        }

        int count = 0;
        LocalDate d = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        while (!d.isAfter(end)) {
            if (targetDays.contains(d.getDayOfWeek())) {
                count++;
                if (count == ordinal) {
                    return Optional.of(d);
                }
            }
            d = d.plusDays(1);
        }

        return Optional.empty(); // 그 달에는 ordinal번째가 없음
    }
}
