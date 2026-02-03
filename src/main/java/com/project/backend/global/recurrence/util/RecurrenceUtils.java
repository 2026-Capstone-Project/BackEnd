package com.project.backend.global.recurrence.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
}
