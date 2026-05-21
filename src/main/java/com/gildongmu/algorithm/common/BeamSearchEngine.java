/*
 * BeamSearchEngine
 * - 빔서치의 공통 실행 흐름을 담당한다.
 * - 후보 조회, 기본 점수 계산, 반복 루프, 완료 판정, 유효 루트 필터링을 수행한다.
 * - 초기화와 후보 선택 방식은 BeamSearchStrategy에 위임한다.
 */
package com.gildongmu.algorithm.common;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.CandidateScore;
import com.gildongmu.algorithm.GeoDataCache;
import com.gildongmu.algorithm.HaversineUtil;
import com.gildongmu.algorithm.ScoreCalculator;
import com.gildongmu.database.entity.JejuRoadPoint;
import com.gildongmu.database.entity.Obstacle;
import com.gildongmu.database.entity.Place;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BeamSearchEngine {

    private final GeoDataCache cache;
    private final BeamSearchConfig config = new BeamSearchConfig();

    public List<BeamState> generate(double startLat, double startLng,
                                     double targetDistance,
                                     Double waypointLat, Double waypointLng,
                                     BeamSearchStrategy strategy) {

        double tolerance = targetDistance * config.getToleranceRatio();

        double[] target = (waypointLat != null)
            ? new double[]{waypointLat, waypointLng}
            : generateAnchorPoint(startLat, startLng, targetDistance);

        double targetLat = target[0];
        double targetLng = target[1];

        BeamSearchContext context = new BeamSearchContext(
            startLat, startLng,
            targetLat, targetLng,
            targetDistance, tolerance,
            config
        );

        logStart(context, strategy);

        JejuRoadPoint startPoint = cache
            .findNearestRoadPoint(startLat, startLng)
            .orElseThrow(() -> new RuntimeException("출발점 근처 도로 없음"));

        List<BeamState> beams = strategy.initializeBeams(startPoint, context);

        int iterations = 0;
        while (beams.stream().anyMatch(b -> !b.isCompleted())
            && iterations++ < config.getMaxIterations()) {

            logIteration(iterations, beams, targetDistance);

            List<BeamState> completedBeams = beams.stream()
                .filter(BeamState::isCompleted)
                .collect(Collectors.toList());

            List<CandidateScore> allCandidates = buildCandidates(beams, context, strategy);
            List<BeamState> newBeams = new ArrayList<>(completedBeams);

            if (!allCandidates.isEmpty()) {
                int remaining = Math.max(0, strategy.getBeamWidth(context) - completedBeams.size());
                List<CandidateScore> top = strategy.selectCandidates(allCandidates, remaining, context);

                for (CandidateScore cs : top) {
                    BeamState newBeam = cs.getSourceBeam().clone();
                    JejuRoadPoint cand = cs.getCandidate();
                    newBeam.addPoint(cand, cs.getSegmentDistance(), cs.getScore());

                    double distToTarget = HaversineUtil.calculate(
                        cand.getLatitude(), cand.getLongitude(), context.getTargetLat(), context.getTargetLng()
                    );
                    if (distToTarget < config.getTargetPassRadius()) {
                        newBeam.setTargetPassed(true);
                    }

                    if (newBeam.getTraveledDistance() >= targetDistance - tolerance) {
                        newBeam.setCompleted(true);
                    }

                    newBeams.add(newBeam);
                }
            }

            if (newBeams.isEmpty()) {
                break;
            }
            beams = newBeams;
        }

        List<BeamState> result = beams.stream()
            .filter(b -> isLoopValid(b, startLat, startLng, targetDistance, tolerance))
            .collect(Collectors.toList());

        List<BeamState> selected = strategy.selectFinalRoutes(result, context);

        logResult(iterations, selected);
        return selected;
    }

    private List<CandidateScore> buildCandidates(
        List<BeamState> beams,
        BeamSearchContext context,
        BeamSearchStrategy strategy
    ) {
        List<CandidateScore> allCandidates = new ArrayList<>();

        for (BeamState beam : beams) {
            if (beam.isCompleted()) {
                continue;
            }

            JejuRoadPoint current = beam.getCurrentPoint();
            double curLat = current.getLatitude();
            double curLng = current.getLongitude();
            double curElev = current.getElevationMeters() != null
                ? current.getElevationMeters() : 0.0;

            List<JejuRoadPoint> candidates = cache.findRoadPointsWithinRadius(
                curLat, curLng, context.getConfig().getStepRadius()
            );
            List<Obstacle> obstacles = cache.findObstaclesWithinRadius(
                curLat, curLng, context.getConfig().getObstacleQueryRadius()
            );
            List<Place> places = cache.findPlacesWithinRadius(
                curLat, curLng, context.getConfig().getPlaceQueryRadius()
            );

            JejuRoadPoint prev = beam.getPrevPoint();
            Double prevLat = prev != null ? prev.getLatitude() : null;
            Double prevLng = prev != null ? prev.getLongitude() : null;

            for (JejuRoadPoint candidate : candidates) {
                double candLat = candidate.getLatitude();
                double candLng = candidate.getLongitude();

                double segDist = HaversineUtil.calculate(curLat, curLng, candLat, candLng);
                if (segDist < context.getConfig().getMinStepDistance()) {
                    continue;
                }

                double predicted = beam.getTraveledDistance() + segDist;
                double returnDist = HaversineUtil.calculate(candLat, candLng, context.getStartLat(), context.getStartLng());

                if (predicted + returnDist > context.getTargetDistance() + context.getTolerance()) {
                    continue;
                }

                double baseScore = ScoreCalculator.calculate(
                    curLat, curLng, curElev,
                    prevLat, prevLng,
                    candidate,
                    context.getStartLat(), context.getStartLng(),
                    context.getTargetLat(), context.getTargetLng(),
                    beam.getTraveledDistance(), context.getTargetDistance(),
                    obstacles, places,
                    beam.getVisitedPoints(),
                    beam.isTargetPassed()
                );

                double adjustedScore = strategy.adjustScore(baseScore, beam, candidate, beams, context);
                allCandidates.add(new CandidateScore(beam, candidate, adjustedScore, segDist));
            }
        }

        return allCandidates;
    }

    private boolean isLoopValid(BeamState beam, double startLat, double startLng,
                                double targetDist, double tolerance) {
        if (beam.getRoute().isEmpty()) {
            return false;
        }
        JejuRoadPoint last = beam.getCurrentPoint();
        double distToStart = HaversineUtil.calculate(
            last.getLatitude(), last.getLongitude(), startLat, startLng
        );
        double total = beam.getTraveledDistance() + distToStart;
        return Math.abs(total - targetDist) <= tolerance;
    }

    private double[] generateAnchorPoint(double startLat, double startLng, double targetDistance) {
        double anchorDist = targetDistance / 2.0;
        double[] angles = {0, 45, 90, 135, 180, 225, 270, 315};
        double bestScore = Double.NEGATIVE_INFINITY;
        double[] bestAnchor = {startLat, startLng + anchorDist / 111000.0};

        for (double angle : angles) {
            double rad = Math.toRadians(angle);
            double candLat = startLat + (anchorDist / 111000.0) * Math.cos(rad);
            double candLng = startLng + (anchorDist / 111000.0) * Math.sin(rad);

            List<JejuRoadPoint> nearby = cache.findRoadPointsWithinRadius(candLat, candLng, 100.0);
            if (nearby.isEmpty()) {
                continue;
            }

            List<Obstacle> obs = cache.findObstaclesWithinRadius(candLat, candLng, 200.0);
            double score = -obs.size();

            if (score > bestScore) {
                bestScore = score;
                bestAnchor = new double[]{candLat, candLng};
            }
        }
        return bestAnchor;
    }

    private void logStart(BeamSearchContext context, BeamSearchStrategy strategy) {
        log.info("┌─────────────────────────────────────────────");
        log.info("│ 빔서치 시작 ({})", strategy.getName());
        log.info("│ 출발점   : ({}, {})",
            String.format("%.5f", context.getStartLat()),
            String.format("%.5f", context.getStartLng()));
        log.info("│ 목표거리  : {}m  (허용오차 ±{}m)",
            (int) context.getTargetDistance(), (int) context.getTolerance());
        log.info("│ 앵커포인트: ({}, {})",
            String.format("%.5f", context.getTargetLat()),
            String.format("%.5f", context.getTargetLng()));
        log.info("└─────────────────────────────────────────────");
    }

    private void logIteration(int iterations, List<BeamState> beams, double targetDistance) {
        if (iterations % 10 != 1) {
            return;
        }

        log.info("[이터 {}/{}] ─────────────────────",
            String.format("%3d", iterations), config.getMaxIterations());
        for (int i = 0; i < beams.size(); i++) {
            BeamState b = beams.get(i);
            String status = b.isCompleted() ? "완료 ✓"
                : b.isTargetPassed() ? "귀환중 ↩"
                : "탐색중 →";
            log.info("  빔#{} | {}m / {}m | {}개 포인트 | {}",
                i,
                String.format("%5d", (int) b.getTraveledDistance()),
                (int) targetDistance,
                b.getRoute().size(),
                status
            );
        }
    }

    private void logResult(int iterations, List<BeamState> result) {
        log.info("┌─────────────────────────────────────────────");
        log.info("│ 빔서치 완료  ({}이터 / 유효루트 {}개)", iterations, result.size());
        for (int i = 0; i < result.size(); i++) {
            BeamState b = result.get(i);
            log.info("│ 루트#{} | 총거리={}m | 포인트={}개 | 총점={}", i,
                String.format("%5d", (int) b.getTraveledDistance()),
                b.getRoute().size(),
                String.format("%.2f", b.getTotalScore())
            );
        }
        if (result.isEmpty()) {
            log.warn("│ ⚠ 유효한 루트 없음 — 조건 완화 필요");
        }
        log.info("└─────────────────────────────────────────────");
    }
}

