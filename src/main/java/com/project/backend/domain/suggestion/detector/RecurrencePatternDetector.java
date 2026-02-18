package com.project.backend.domain.suggestion.detector;

import com.project.backend.domain.suggestion.detector.vo.*;
import com.project.backend.domain.suggestion.enums.StableType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RecurrencePatternDetector {

    public Optional<DetectionResult> detect(RecurrencePreprocessResult result) {

        List<DetectionResult> candidates = new ArrayList<>();

        detectNInterval(result.dayDiff()).ifPresent(candidates::add);
        log.info("interval: ={}", result.dayDiff());
        detectWeeklySet(result.weeklyHistory()).ifPresent(candidates::add);
        log.info("week: ={}", result.weeklyHistory());
        detectMonthlyDay(result.monthlyHistory()).ifPresent(candidates::add);
        log.info("monthly: ={}", result.monthlyHistory());

        Comparator<DetectionResult> comp = Comparator
                .comparingInt(DetectionResult::score)
                .thenComparingInt(dr -> trailingCount(dr, result));

        Optional<DetectionResult> winnerOpt = candidates.stream().max(comp);
        if (winnerOpt.isEmpty()) return Optional.empty(); // candidates 비어있음

        DetectionResult winner = winnerOpt.get();

        // 가장 높은 점수를 가진 유일한 객체인가
        long tieCount = candidates.stream()
                .filter(dr -> comp.compare(dr, winner) == 0)
                .count();
        if (tieCount > 1) {
            // 아직 경합 상태 -> 결정 불가
            log.info("AMBIGUOUS TYPE winner={}, tiedCount={}", winner, tieCount);
            return Optional.empty();
        }
        // 승자
        return Optional.of(winner);
    }
    private int trailingCount(DetectionResult dr, RecurrencePreprocessResult result) {
        return switch (dr.patternType()) { // 네 enum 이름에 맞춰 변경
            case N_INTERVAL -> countTrailingSame(lastN(result.dayDiff(), 5));

            case WEEKLY_SET -> countTrailingSame(lastN(result.weeklyHistory().dayOfWeekSet(), 5));
            // dayOfWeekSet: List<Set<DayOfWeek>>

            case MONTHLY_DAY -> countTrailingSame(lastN(result.monthlyHistory().dayOfMonthSet(), 5));
            // dayOfMonth: List<Set<Integer>>
        };
    }

    private <T> List<T> lastN(List<T> list, int n) {
        if (list == null) return List.of();
        int size = list.size();
        if (size <= n) return list;
        return list.subList(size - n, size);
    }


    private Optional<DetectionResult> detectNInterval(List<Integer> dayDiff) {

        // 판단 객체가 너무 적음
        if (dayDiff.size() < 2) {
            return Optional.empty();
        }

        List<Integer> slicedDayDiff = sliceLastElement(dayDiff);

        int dayDiffTrailingCnt = countTrailingSame(dayDiff);
        int slicedDayDiffTrailingCnt = countTrailingSame(slicedDayDiff);

        // 완전 stable: 마지막 기준 연속 2개 이상
        // ex) [7, 7], [7, 7, 7]
        if (dayDiffTrailingCnt >= 2) {
            return Optional.of(
                    DetectionResult.nInterval(StableType.PERFECTLY_STABLE, dayDiff.getLast(), null)
            );
        }
        // stable이 깨진 첫 순간 (candidate 4개 이상 && dayDiff 길이 3이상)
        // ex) [7, 7, 14]
        else if (slicedDayDiffTrailingCnt >= 2) {
            return Optional.of(
                    DetectionResult.nInterval(StableType.PARTIALLY_STABLE, slicedDayDiff.getLast(), dayDiff.getLast())
            );
        }
        // 이전에 강한 패턴이 있었으나 오염된 상태 (candidate 6개 이상 && dayDiff 길이 5이상)
        // ex) [7, 7, 7, 14, 7]
        else if (dayDiff.size() >= 5) {
            Integer mode = getMode(dayDiff);
            if (mode != null) {
                return Optional.of(
                        DetectionResult.nInterval(StableType.CONTAMINATED_STABLE, mode, null)
                );
            }
        }
        // 아무것도 포함되지 않으면 패턴 없음
        return Optional.empty();
    }

    private Optional<DetectionResult> detectWeeklySet(WeeklyHistory weeklyHistory) {

        List<Set<DayOfWeek>> dayOfWeekSet = weeklyHistory.dayOfWeekSet();
        List<Integer> weekDiff = weeklyHistory.weekDiff();

        // 판단 객체가 너무 적음
        if (weekDiff.size() < 2) {
            return Optional.empty();
        }

        List<Set<DayOfWeek>> slicedDayOfWeekSet = sliceLastElement(dayOfWeekSet);
        List<Integer> slicedWeekDiff = sliceLastElement(weekDiff);

        int dayOfWeekTrailingCnt = countTrailingSame(dayOfWeekSet);
        int slicedDayOfWeekTrailingCnt = countTrailingSame(slicedDayOfWeekSet);

        int weekDiffTrailingCnt = countTrailingSame(weekDiff);
        int slicedWeekDiffTrailingCnt = countTrailingSame(slicedWeekDiff);

        // 완전 stable: 마지막 기준 연속 2개 이상 & 각각의 weekDiff도 마지막 기준 연속 2개 이상
        // ex) [{월, 화}, {월, 화}], [{월}, {월}, {월}]
        if (dayOfWeekTrailingCnt >= 2 && weekDiffTrailingCnt >= 2) {
            return Optional.of(
                    DetectionResult.weeklySet(
                            StableType.PERFECTLY_STABLE,
                            new WeeklyPattern(weekDiff.getLast(), dayOfWeekSet.getLast()),
                            null
                    )
            );
        }
        // stable이 깨진 첫 순간 (dayOfWeekSet 길이 3이상)
        // ex) [{월, 화}, {월, 화}, {화}]
        else if (slicedDayOfWeekTrailingCnt >= 2 && slicedWeekDiffTrailingCnt >= 2) {
            return Optional.of(
                    DetectionResult.weeklySet(
                            StableType.PARTIALLY_STABLE,
                            new WeeklyPattern(slicedWeekDiff.getLast(), slicedDayOfWeekSet.getLast()),
                            new WeeklyPattern(weekDiff.getLast(), dayOfWeekSet.getLast())
                    )
            );
        }
        // 이전에 강한 패턴이 있었으나 오염된 상태 (dayOfWeekSet 길이 5이상)
        // ex) [{월}, {월}, {월}, {화}, {월}]
        else if (weekDiff.size() >= 5) {
            Integer weekDiffMode = getMode(weekDiff);
            Set<DayOfWeek> dayOfWeekMode = getMode(dayOfWeekSet);
            if (dayOfWeekMode != null && weekDiffMode != null) {
                return Optional.of(
                        DetectionResult.weeklySet(
                                StableType.CONTAMINATED_STABLE,
                                new WeeklyPattern(weekDiffMode, dayOfWeekMode),
                                null
                        )
                );
            }
        }

        return Optional.empty();
    }

    private Optional<DetectionResult> detectMonthlyDay(MonthlyHistory monthlyHistory) {

        List<Set<Integer>> dayOfMonthSet = monthlyHistory.dayOfMonthSet();
        List<Integer> monthDiff = monthlyHistory.monthDiff();

        // 판단 객체가 너무 적음
        if (monthDiff.size() < 2) {
            return Optional.empty();
        }

        List<Set<Integer>> slicedDayOfMonthSet = sliceLastElement(dayOfMonthSet);
        List<Integer> slicedMonthDiff = sliceLastElement(monthDiff);

        int dayOfMonthTrailingCnt = countTrailingSame(dayOfMonthSet);
        int slicedDayOfMonthTrailingCnt = countTrailingSame(slicedDayOfMonthSet);

        int monthDiffTrailingCnt = countTrailingSame(monthDiff);
        int slicedMonthDiffTrailingCnt = countTrailingSame(slicedMonthDiff);

        // 완전 stable: 마지막 기준 연속 2개 이상
        if (dayOfMonthTrailingCnt >= 2 && monthDiffTrailingCnt >= 2) {
            return Optional.of(
                    DetectionResult.monthlyDay(
                            StableType.PERFECTLY_STABLE,
                            new MonthlyPattern(monthDiff.getLast(), dayOfMonthSet.getLast()),
                            null
                    )
            );
        }
        // stable이 깨진 첫 순간
        else if (slicedDayOfMonthTrailingCnt >= 2 && slicedMonthDiffTrailingCnt >= 2) {
            return Optional.of(
                    DetectionResult.monthlyDay(
                            StableType.PARTIALLY_STABLE,
                            new MonthlyPattern(slicedMonthDiff.getLast(), slicedDayOfMonthSet.getLast()),
                            new MonthlyPattern(monthDiff.getLast(), dayOfMonthSet.getLast())
                    )
            );
        }
        // 이전에 강한 패턴이 있었으나 오염된 상태
        else if (monthDiff.size() >= 5) {
            Integer monthDiffMode = getMode(monthDiff);
            Set<Integer> dayOfMonthMode = getMode(dayOfMonthSet);
            if (monthDiffMode != null && dayOfMonthMode != null) {
                return Optional.of(
                        DetectionResult.monthlyDay(
                                StableType.CONTAMINATED_STABLE,
                                new MonthlyPattern(monthDiffMode, dayOfMonthMode),
                                null
                        )
                );
            }
        }

        return Optional.empty();
    }

    private <T> List<T> sliceLastElement(List<T> diff) {
        return diff.subList(0, diff.size() - 1);
    }

    private <T> int countTrailingSame(List<T> diff) {

        if (diff.isEmpty()) return 0;

        T last = diff.getLast();
        int count = 1;

        for (int i = diff.size() - 2; i >= 0; i--) {
            if (Objects.equals(diff.get(i), last)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }


    private <T>T getMode(List<T> dayDiff) {

        List<T> recentDiff = dayDiff.subList(dayDiff.size() - 5, dayDiff.size());

        // 최근 5개의 패턴을 그룹핑
        Map<T, Long> freq = recentDiff.stream()
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

        // 그 중에서 가장 많은 값을 추출
        long maxCount = freq.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(-1);

        // maxCount를 가지는 Key(Diff)를 추출
        List<T> modes = freq.entrySet().stream()
                .filter(e -> e.getValue() == maxCount)
                .map(Map.Entry::getKey)
                .toList();

        if (modes.size() == 1 && maxCount > recentDiff.size() / 2.0) {
            return modes.getFirst();
        }
        return null;
    }

}
