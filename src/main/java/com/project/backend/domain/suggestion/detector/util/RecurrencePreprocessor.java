package com.project.backend.domain.suggestion.detector.util;

import com.project.backend.domain.suggestion.detector.vo.MonthlyHistory;
import com.project.backend.domain.suggestion.detector.vo.RecurrencePreprocessResult;
import com.project.backend.domain.suggestion.detector.vo.WeeklyHistory;
import com.project.backend.domain.suggestion.vo.SuggestionCandidate;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;

import static com.project.backend.domain.suggestion.util.WeekEpochUtil.toEpochWeek;

@Slf4j
public class RecurrencePreprocessor {

    public static RecurrencePreprocessResult preprocess(List<SuggestionCandidate> candidateList) {
        // TODO : candidateList가 널이거나 길이가 짧으면?

        // 명시적 정렬
        List<SuggestionCandidate> sorted = candidateList.stream()
                .sorted(Comparator.comparing(SuggestionCandidate::start))
                .toList();

        // DayDiff 계산
        List<Integer> dayDiff = getDayDiff(sorted);

        // DayOfWeekSet 계산
        WeeklyHistory weeklyHistory = getWeeklyPattern(sorted);

        // DayOfMonth 계산
        MonthlyHistory monthlyHistory = getMonthlyPattern(sorted);

        return new RecurrencePreprocessResult(dayDiff, weeklyHistory, monthlyHistory);

    }

    // 각 이벤트의 날짜 차이를 리스트로 반환 [7, 7, 7, 7]
    private static List<Integer> getDayDiff(List<SuggestionCandidate> candidateList) {

        List<Integer> diff = new ArrayList<>();
        LocalDate base;
        LocalDate next;

        for (int i = 0; i < candidateList.size() - 1; i++) {
            base = candidateList.get(i).start().toLocalDate();
            next = candidateList.get(i + 1).start().toLocalDate();
            diff.add((int) ChronoUnit.DAYS.between(base, next));
        }
        return diff;
    }

    private static WeeklyHistory getWeeklyPattern(List<SuggestionCandidate> candidateList) {

        Map<Long, Set<DayOfWeek>> weekMap = new LinkedHashMap<>();

        // 후보 이벤트가 년 기준으로 몇 주차의 무슨 요일인지 판단하여 집합으로 저장
        for (SuggestionCandidate c : candidateList) {
            LocalDate start = c.start().toLocalDate();
            // ex) 2026년 15주차 -> 기준 주로 부터 얼마나 떨어져 있었는지
            long epochWeek = toEpochWeek(start);

            // weekKey가 처음 등장하면 빈 집합을 만들어서 후보 이벤트의 요일을 추가
            weekMap
                    .computeIfAbsent(epochWeek, k -> new LinkedHashSet<>())
                    .add(start.getDayOfWeek());
        }

        List<Set<DayOfWeek>> dayOfWeekSets = new ArrayList<>(weekMap.values());
        List<Long> epochWeeks = new ArrayList<>(weekMap.keySet());

        List<Integer> weekDiffs = new ArrayList<>();
        // 인접한 epoch 들의 차이로 요일 셋의 주 간격을 알아낸다
        for (int i = 0; i < epochWeeks.size() - 1; i++) {
            weekDiffs.add((int) (epochWeeks.get(i + 1) - epochWeeks.get(i)));
        }

        return new WeeklyHistory(weekDiffs, dayOfWeekSets);
    }

    private static MonthlyHistory getMonthlyPattern(List<SuggestionCandidate> candidateList) {
        Map<YearMonth, Set<Integer>> monthMap = new LinkedHashMap<>();

        for (SuggestionCandidate c : candidateList) {
            LocalDate start = c.start().toLocalDate();
            YearMonth yearMonth = YearMonth.from(start);

            monthMap
                    .computeIfAbsent(yearMonth, k -> new LinkedHashSet<>())
                    .add(start.getDayOfMonth());
        }

        List<Set<Integer>> dayOfMonthSet = new ArrayList<>(monthMap.values());
        List<YearMonth> months = new ArrayList<>(monthMap.keySet());

        List<Integer> monthDiffHistory = new ArrayList<>();
        for (int i = 0; i < months.size() - 1; i++) {
            monthDiffHistory.add(
                    (int) ChronoUnit.MONTHS.between(months.get(i), months.get(i + 1))
            );
        }

        return new MonthlyHistory(monthDiffHistory, dayOfMonthSet);
    }
}
