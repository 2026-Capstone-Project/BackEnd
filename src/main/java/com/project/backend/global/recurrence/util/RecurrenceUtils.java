package com.project.backend.global.recurrence.util;

import com.project.backend.domain.common.plan.enums.MonthlyWeekdayRule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
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
     * 월 반복(DAY_OF_WEEK)에서 저장된 요일 집합을 기반으로
     * 해당 집합이 어떤 MonthlyWeekdayRule에 해당하는지 역추론한다.
     *
     * 동작 방식:
     *  1. 전달된 요일 리스트에서 null 및 중복을 제거한다.
     *  2. 요일 개수 및 구성(Set)을 기준으로 rule을 판별한다.
     *
     * 판별 규칙:
     *  - 1개          → SINGLE
     *  - [SATURDAY, SUNDAY] → WEEKEND
     *  - [MONDAY ~ FRIDAY]  → WEEKDAY
     *  - [MONDAY ~ SUNDAY]  → ALL_DAYS
     *
     * 예외 처리:
     *  - null 또는 빈 리스트 → IllegalArgumentException
     *  - 위 규칙에 해당하지 않는 임의 조합 → IllegalArgumentException
     *
     * @param dayOfWeekInMonth 월 반복에서 사용된 요일 목록
     * @return 추론된 MonthlyWeekdayRule
     */
    public static MonthlyWeekdayRule inferWeekdayRule(List<DayOfWeek> dayOfWeekInMonth) {
        if (dayOfWeekInMonth == null || dayOfWeekInMonth.isEmpty()) {
            throw new IllegalArgumentException("dayOfWeekInMonth is empty");
        }

        // 중복 제거 + null 방지
        EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeek d : dayOfWeekInMonth) {
            if (d == null) throw new IllegalArgumentException("dayOfWeekInMonth contains null");
            set.add(d);
        }

        if (set.size() == 1) return MonthlyWeekdayRule.SINGLE;
        if (set.equals(WEEKEND_SET)) return MonthlyWeekdayRule.WEEKEND;
        if (set.equals(WEEKDAY_SET)) return MonthlyWeekdayRule.WEEKDAY;
        if (set.equals(ALL_DAYS_SET)) return MonthlyWeekdayRule.ALL_DAYS;

        throw new IllegalArgumentException("Unsupported dayOfWeekInMonth set: " + set);
    }

    /**
     * MonthlyWeekdayRule과 단일 요일 정보를 기반으로
     * DB에 저장할 dayOfWeekInMonth 문자열을 생성한다.
     *
     * 동작 방식:
     *  - SINGLE  → singleDay 1개를 문자열로 반환 ("MONDAY")
     *  - WEEKDAY → "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"
     *  - WEEKEND → "SATURDAY,SUNDAY"
     *  - ALL_DAYS → "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY"
     *
     * 정책:
     *  - rule이 null이고 singleDay가 존재하면 하위 호환으로 단일 요일로 처리한다.
     *  - SINGLE인데 singleDay가 null이면 IllegalArgumentException 발생
     *
     * 사용 목적:
     *  - DTO → Entity 변환 시 weekdayRule을 실제 저장 문자열로 정규화하기 위함
     *
     * @param rule        적용할 MonthlyWeekdayRule
     * @return 콤마로 구분된 요일 문자열 (DB 저장용)
     */
    public static String normalizeDayOfWeekInMonth(MonthlyWeekdayRule rule) {
        return switch (rule) {
            case SINGLE -> throw new IllegalArgumentException("SINGLE requires dayOfWeekInMonth");
            case WEEKDAY -> "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";
            case WEEKEND -> "SATURDAY,SUNDAY";
            case ALL_DAYS -> "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";
        };
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
     * MONTHLY - DAY_OF_WEEK (ORDINAL 기반) 규칙에 따라
     * 특정 월에서 "n번째(ordinal)" 요일을 계산한다.
     * 정의:
     *  - weekOfMonth 값은 "n번째 주차"가 아니라,
     *    해당 월 안에서 조건을 만족하는 날짜의 "n번째 등장 순서"를 의미한다.
     * 동작 방식:
     *  1. 해당 월의 1일부터 말일까지 날짜를 순차적으로 탐색한다.
     *  2. 각 날짜의 요일이 targetDays에 포함되는 경우만 카운트한다.
     *  3. 조건을 만족하는 날짜가 ordinal번째에 도달하면 해당 날짜를 반환한다.
     * 예시:
     *  - 2026년 4월, ordinal=1, targetDays=[FRIDAY]
     *    → 2026-04-03 반환 (해당 월의 첫 번째 금요일)
     *  - 2026년 2월, ordinal=5, targetDays=[MONDAY]
     *    → Optional.empty()
     *      (2026년 2월에는 월요일이 4번만 존재)
     *  - 2026년 2월, ordinal=5, targetDays=[MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY]
     *    → 2026-02-06 반환 (해당 월의 5번째 평일)
     * 반환 정책 (SKIP):
     *  - 해당 월에 ordinal번째 조건 만족 날짜가 존재하면 → 해당 날짜 반환
     *  - 존재하지 않으면 → Optional.empty() 반환
     *    (다음 달 계산은 상위 로직에서 처리)
     *
     * @param month       대상 연-월
     * @param ordinal     n번째 (1부터 시작)
     * @param targetDays  허용된 요일 목록 (단일/주중/주말/전체/다중 선택 가능)
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

    // =============== private ===================

    private static final Set<DayOfWeek> WEEKDAY_SET = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );
    private static final Set<DayOfWeek> WEEKEND_SET = EnumSet.of(
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    );
    private static final Set<DayOfWeek> ALL_DAYS_SET = EnumSet.allOf(DayOfWeek.class);
}
