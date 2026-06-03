/*
 * DefaultBeamSearchStrategy
 * - 빔 1개로 시작해 탐색 중 전역 상위 k=7 경쟁 유지 (순수 빔서치).
 * - 최종 단계에서 겹침 30% 초과 경로를 제거하고 상위 3개를 선택한다.
 */
package com.gildongmu.algorithm.basic;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.CandidateScore;
import com.gildongmu.algorithm.HaversineUtil;
import com.gildongmu.algorithm.common.BeamSearchContext;
import com.gildongmu.algorithm.common.BeamSearchStrategy;
import com.gildongmu.database.entity.JejuRoadPoint;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DefaultBeamSearchStrategy implements BeamSearchStrategy {

    @Override
    public String getName() {
        return "DEFAULT";
    }

    @Override
    public int getBeamWidth(BeamSearchContext context) {
        return context.getConfig().getExplorationBeamWidth();
    }

    @Override
    public List<BeamState> initializeBeams(JejuRoadPoint startPoint, BeamSearchContext context) {
        BeamState beam = new BeamState();
        beam.setBeamIndex(0);
        beam.addPoint(startPoint, 0.0, 0.0);
        return Collections.singletonList(beam);
    }

    @Override
    public double adjustScore(
        double baseScore,
        BeamState sourceBeam,
        JejuRoadPoint candidate,
        List<BeamState> currentBeams,
        BeamSearchContext context
    ) {
        // 다른 빔이 이미 지난 포인트 근처면 점수 감점 → 탐색 중 경로 분산 유도
        double diversityPenalty = currentBeams.stream()
            .filter(b -> b.getBeamIndex() != sourceBeam.getBeamIndex())
            .flatMap(b -> b.getRoute().stream())
            .mapToDouble(p -> HaversineUtil.calculate(
                candidate.getLatitude(), candidate.getLongitude(),
                p.getLatitude(), p.getLongitude()))
            .filter(d -> d < 30.0)
            .count() * 5.0;

        return baseScore - diversityPenalty;
    }

    @Override
    public List<CandidateScore> selectCandidates(
        List<CandidateScore> candidates,
        int limit,
        BeamSearchContext context
    ) {
        // 순수 전역 상위 k — 빔당 생존 보장 없이 경쟁
        return candidates.stream()
            .sorted(Comparator.comparingDouble(CandidateScore::getScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<BeamState> selectFinalRoutes(List<BeamState> routes, BeamSearchContext context) {
        int finalCount = context.getConfig().getBeamWidth(); // 3

        List<BeamState> sorted = routes.stream()
            .sorted(Comparator.comparingDouble(BeamState::getTotalScore).reversed())
            .collect(Collectors.toList());

        // 점수 높은 순으로 추가하되, 이미 선택된 경로와 30% 이상 겹치면 제외
        List<BeamState> selected = new ArrayList<>();
        for (BeamState candidate : sorted) {
            boolean tooSimilar = selected.stream()
                .anyMatch(s -> overlapRatio(s, candidate) > 0.3);
            if (!tooSimilar) {
                selected.add(candidate);
            }
            if (selected.size() >= finalCount) break;
        }

        // 다양한 경로가 부족하면 점수 순으로 채움
        if (selected.size() < finalCount) {
            sorted.stream()
                .filter(r -> !selected.contains(r))
                .limit(finalCount - selected.size())
                .forEach(selected::add);
        }

        return selected;
    }

    private double overlapRatio(BeamState a, BeamState b) {
        Set<Integer> idsA = a.getRoute().stream()
            .map(JejuRoadPoint::getPointId)
            .collect(Collectors.toSet());
        long overlap = b.getRoute().stream()
            .filter(p -> idsA.contains(p.getPointId()))
            .count();
        int minSize = Math.min(a.getRoute().size(), b.getRoute().size());
        return minSize == 0 ? 0.0 : (double) overlap / minSize;
    }
}
