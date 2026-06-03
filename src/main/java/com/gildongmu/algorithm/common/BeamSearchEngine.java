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
import java.util.Map;
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
                                     BeamSearchStrategy strategy,
                                     double slopeWeight, double slopeThreshold) {

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
            config,
            cache
        );

        logStart(context, strategy);

        JejuRoadPoint startPoint = cache
            .findNearestRoadPoint(startLat, startLng)
            .orElseThrow(() -> new RuntimeException("출발점 근처 도로 없음"));

        // ── Phase 1: 방향 목표를 향해 탐색, 실제 종점이 앵커가 됨 ──────
        log.info("│ [Phase 1] 출발 → 방향 탐색 시작 (sideB=false)");
        List<BeamState> beams = strategy.initializeBeams(startPoint, context);
        beams.forEach(b -> {
            b.setAnchor(startLat, startLng); // 출발점 기준 → 멀어지는 방향으로 퍼짐 (종점기준)
            b.setSideB(false);
        });

        int phase1MaxIter = config.getMaxIterations() / 2;
        int iter1 = 0;
        while (beams.stream().anyMatch(b -> !b.isCompleted()) && iter1++ < phase1MaxIter) {
            logIteration(iter1, beams, targetDistance / 2);

            List<BeamState> completedBeams = beams.stream()
                .filter(BeamState::isCompleted).collect(Collectors.toList());
            List<CandidateScore> allCandidates = buildCandidates(beams, context, strategy, slopeWeight, slopeThreshold);
            List<BeamState> newBeams = new ArrayList<>(completedBeams);

            if (!allCandidates.isEmpty()) {
                int remaining = Math.max(0, strategy.getBeamWidth(context) - completedBeams.size());
                for (CandidateScore cs : strategy.selectCandidates(allCandidates, remaining, context)) {
                    BeamState newBeam = cs.getSourceBeam().clone();
                    JejuRoadPoint cand = cs.getCandidate();
                    newBeam.addPoint(cand, cs.getSegmentDistance(), cs.getScore());

                    // Phase1 종료: 이동거리 48% 이상 AND 출발점 직선거리 32% 이상
                    // → 앵커가 출발점 근처에 찍히지 않도록 보장 (기존 0.38 기준 유지)
                    double distFromStart = HaversineUtil.calculate(
                        startLat, startLng, cand.getLatitude(), cand.getLongitude());
                    boolean traveledEnough = newBeam.getTraveledDistance() >= targetDistance * 0.48;
                    boolean farEnough = distFromStart >= 2000.0;
                    if (traveledEnough && farEnough) {
                        newBeam.setTargetPassed(true);
                        newBeam.setPhaseOneBoundary(newBeam.getRoute().size());
                        newBeam.setCompleted(true);
                    } else if (newBeam.getTraveledDistance() >= targetDistance * 0.51) {
                        // 폴백: 너무 멀리 갔으면 직선거리 조건 무시하고 강제 종료
                        newBeam.setPhaseOneBoundary(newBeam.getRoute().size());
                        newBeam.setCompleted(true);
                    }
                    newBeams.add(newBeam);
                }
            }
            if (newBeams.isEmpty()) break;
            beams = newBeams;
        }

        List<BeamState> phase1Done = beams.stream()
            .filter(BeamState::isCompleted).collect(Collectors.toList());
        if (phase1Done.isEmpty()) phase1Done = new ArrayList<>(beams);

        // Phase1 상위 3개 빔 → 각각 다른 앵커로 Phase2 독립 실행
        List<BeamState> p1Top = phase1Done.stream()
            .sorted(java.util.Comparator.comparingDouble(BeamState::getTotalScore).reversed())
            .limit(config.getBeamWidth())
            .collect(Collectors.toList());

        log.info("│ [Phase1 완료] 앵커 후보 {}개", p1Top.size());
        for (int i = 0; i < p1Top.size(); i++) {
            JejuRoadPoint ap = p1Top.get(i).getCurrentPoint();
            p1Top.get(i).setAnchor(ap.getLatitude(), ap.getLongitude());
            log.info("│  앵커{}: id={} ({}, {})", i,
                ap.getPointId(),
                String.format("%.5f", ap.getLatitude()),
                String.format("%.5f", ap.getLongitude()));
        }

        // 각 P1 빔마다 Phase2 독립 실행 → 조합
        List<BeamState> combined = new ArrayList<>();
        for (BeamState p1 : p1Top) {
            JejuRoadPoint anchorNode = p1.getCurrentPoint();
            log.info("│ [Phase 2] 앵커 {} 에서 귀환 탐색", anchorNode.getPointId());

            List<BeamState> p2Beams = strategy.initializeBeams(anchorNode, context);
            p2Beams.forEach(b -> {
                b.setAnchor(startLat, startLng);
                b.setSideB(true);
                b.addVisitedHistory(p1.getVisitedPoints());
            });

            int phase2MaxIter = config.getMaxIterations() / 2;
            int iter2 = 0;
            List<BeamState> curBeams = p2Beams;
            while (curBeams.stream().anyMatch(b -> !b.isCompleted()) && iter2++ < phase2MaxIter) {
                List<BeamState> completedBeams = curBeams.stream()
                    .filter(BeamState::isCompleted).collect(Collectors.toList());
                List<CandidateScore> allCandidates = buildCandidates(curBeams, context, strategy, slopeWeight, slopeThreshold);
                List<BeamState> newBeams = new ArrayList<>(completedBeams);

                if (!allCandidates.isEmpty()) {
                    int remaining = Math.max(0, strategy.getBeamWidth(context) - completedBeams.size());
                    for (CandidateScore cs : strategy.selectCandidates(allCandidates, remaining, context)) {
                        BeamState newBeam = cs.getSourceBeam().clone();
                        JejuRoadPoint cand = cs.getCandidate();
                        newBeam.addPoint(cand, cs.getSegmentDistance(), cs.getScore());

                        boolean hitStart = cand.getPointId().equals(startPoint.getPointId());
                        boolean phase2MinDist = newBeam.getTraveledDistance() >= targetDistance * 0.42;
                        if (hitStart && phase2MinDist) {
                            newBeam.setTargetPassed(true);
                            newBeam.setCompleted(true);
                        } else if (newBeam.getTraveledDistance() >= targetDistance * 0.51) {
                            newBeam.setCompleted(true);
                        }
                        newBeams.add(newBeam);
                    }
                }
                if (newBeams.isEmpty()) break;
                curBeams = newBeams;
            }

            List<BeamState> phase2Done = curBeams.stream()
                .filter(BeamState::isTargetPassed).collect(Collectors.toList());
            if (phase2Done.isEmpty()) phase2Done = new ArrayList<>(curBeams);

            // 이 P1과 가장 좋은 P2 조합
            BeamState bestP2 = phase2Done.stream()
                .max(java.util.Comparator.comparingDouble(BeamState::getTotalScore))
                .orElse(null);
            if (bestP2 == null) continue;

            if (!sameAnchorApproach(p1, bestP2) && phaseOverlap(p1, bestP2) <= 0.5) {
                combined.add(combinePhases(p1, bestP2));
            } else {
                // 조건 완화 폴백
                combined.add(combinePhases(p1, bestP2));
            }
        }

        // 거리 트리밍: 목표 초과분을 Phase2 끝(시작점 근처)부터 제거
        combined.forEach(b -> {
            if (b.getTraveledDistance() > targetDistance + tolerance) {
                b.trimToDistance(targetDistance + tolerance * 0.5);
            }
        });

        List<BeamState> result = combined.stream()
            .filter(b -> isLoopValid(b, startLat, startLng, targetDistance, tolerance))
            .collect(Collectors.toList());

        List<BeamState> selected = strategy.selectFinalRoutes(result.isEmpty() ? combined : result, context);

        logResult(iter1, selected);
        return selected;
    }

    private List<CandidateScore> buildCandidates(
        List<BeamState> beams,
        BeamSearchContext context,
        BeamSearchStrategy strategy,
        double slopeWeight, double slopeThreshold
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

            // 그래프 기반 이웃 노드 조회 (실제 연결된 도로만)
            List<JejuRoadPoint> candidates = context.getNeighbors(current.getPointId());
            List<Obstacle> obstacles = cache.findObstaclesWithinRadius(
                curLat, curLng, context.getConfig().getObstacleQueryRadius()
            );
            List<Place> places = cache.findPlacesWithinRadius(
                curLat, curLng, context.getConfig().getPlaceQueryRadius()
            );

            // 현재 페이즈 전체 방문 노드 — 페이즈 내 절대 중복 금지
            List<JejuRoadPoint> route = beam.getRoute();
            java.util.Set<Integer> recentIds = new java.util.HashSet<>();
            for (JejuRoadPoint rp : route) {
                recentIds.add(rp.getPointId());
            }

            // 연속 꺾임 판단용: 직전 방향 벡터 미리 계산
            double prevDirLat = 0, prevDirLng = 0;
            boolean hasPrevDir = false;
            double prevTurnAngle = 0;
            if (route.size() >= 3) {
                JejuRoadPoint r2 = route.get(route.size() - 2);
                JejuRoadPoint r1 = route.get(route.size() - 1);
                double ax = r2.getLatitude()  - route.get(route.size() - 3).getLatitude();
                double ay = r2.getLongitude() - route.get(route.size() - 3).getLongitude();
                double bx = r1.getLatitude()  - r2.getLatitude();
                double by = r1.getLongitude() - r2.getLongitude();
                double ma = Math.sqrt(ax*ax + ay*ay), mb = Math.sqrt(bx*bx + by*by);
                if (ma > 0 && mb > 0) {
                    double cosA = Math.max(-1.0, Math.min(1.0, (ax*bx + ay*by) / (ma*mb)));
                    prevTurnAngle = Math.toDegrees(Math.acos(cosA)); // 직전 꺾임 각도
                }
                prevDirLat = bx; prevDirLng = by;
                hasPrevDir = mb > 0;
            } else if (route.size() == 2) {
                JejuRoadPoint r1 = route.get(route.size() - 1);
                prevDirLat = r1.getLatitude()  - route.get(0).getLatitude();
                prevDirLng = r1.getLongitude() - route.get(0).getLongitude();
                hasPrevDir = (prevDirLat*prevDirLat + prevDirLng*prevDirLng) > 0;
            }

            for (JejuRoadPoint candidate : candidates) {
                // 최근 방문 포인트는 건너뜀 (U턴 / 단기 루프 방지)
                if (recentIds.contains(candidate.getPointId())) {
                    continue;
                }

                // 막다른길(이웃 1개 이하) 제외 — 순환에 기여 못함
                List<JejuRoadPoint> candNeighbors = context.getNeighbors(candidate.getPointId());
                if (candNeighbors.size() <= 1) {
                    continue;
                }

                // 중간점 제외: 후보 이웃 중 현 페이즈 방문 노드가 2개 이상 → 방문 노드 사이에 끼임
                long visitedNeighborCount = candNeighbors.stream()
                    .filter(n -> recentIds.contains(n.getPointId()))
                    .count();
                if (visitedNeighborCount >= 2) {
                    continue;
                }

                // Phase2: Phase1 방문 노드 하드 차단 (10m 이내 = 동일 노드)
                if (beam.isSideB()) {
                    double cLat = candidate.getLatitude(), cLng = candidate.getLongitude();
                    boolean usedByPhase1 = beam.getVisitedPoints().stream()
                        .anyMatch(p -> HaversineUtil.calculate(cLat, cLng, p[0], p[1]) <= 10.0);
                    if (usedByPhase1) continue;
                }

                double candLat = candidate.getLatitude();
                double candLng = candidate.getLongitude();

                // 연속 꺾임 차단: 직전 꺾임 ≥45도 AND 현재 꺾임 ≥45도 → 제외
                if (hasPrevDir && prevTurnAngle >= 45.0) {
                    double cx = candLat - curLat, cy = candLng - curLng;
                    double mc = Math.sqrt(cx*cx + cy*cy);
                    double mp = Math.sqrt(prevDirLat*prevDirLat + prevDirLng*prevDirLng);
                    if (mc > 0 && mp > 0) {
                        double cosC = Math.max(-1.0, Math.min(1.0,
                            (prevDirLat*cx + prevDirLng*cy) / (mp*mc)));
                        double curTurn = Math.toDegrees(Math.acos(cosC));
                        if (curTurn >= 45.0) continue;
                    }
                }

                double segDist = HaversineUtil.calculate(curLat, curLng, candLat, candLng);
                if (segDist < context.getConfig().getMinStepDistance()) {
                    continue;
                }


                double predicted = beam.getTraveledDistance() + segDist;
                // Phase1·Phase2 동일: 55% 이내 (42%~55% 구간에서 앵커 도달 확인)
                double maxBudget = context.getTargetDistance() * 0.55;
                if (predicted > maxBudget) {
                    continue;
                }

                double baseScore = ScoreCalculator.calculate(
                    curLat, curLng, curElev,
                    candidate,
                    slopeWeight, slopeThreshold,
                    context.getStartLat(), context.getStartLng(),
                    beam.getAnchorLat(), beam.getAnchorLng(), // 빔 실제 앵커 기준 방향 점수
                    beam.getTraveledDistance(), context.getTargetDistance(),
                    obstacles, places,
                    beam.getVisitedPoints(),
                    beam.isSideB()
                );

                double adjustedScore = strategy.adjustScore(baseScore, beam, candidate, beams, context);
                allCandidates.add(new CandidateScore(beam, candidate, adjustedScore, segDist));
            }
        }

        return allCandidates;
    }

    // Phase1 앵커 직전 노드 == Phase2 앵커 직후 노드 → 같은 방향으로 진입/탈출 → U턴
    private boolean sameAnchorApproach(BeamState p1, BeamState p2) {
        List<JejuRoadPoint> r1 = p1.getRoute();
        List<JejuRoadPoint> r2 = p2.getRoute();
        if (r1.size() < 2 || r2.size() < 2) return false;

        // Phase1: [..., A, anchor]  → A = r1[-2]
        // Phase2: [anchor, B, ...]  → B = r2[1]  (r2[0] = anchor 시작점)
        int p1LastBeforeAnchor = r1.get(r1.size() - 2).getPointId();
        int p2FirstAfterAnchor = r2.get(Math.min(1, r2.size() - 1)).getPointId();
        return p1LastBeforeAnchor == p2FirstAfterAnchor;
    }

    private double phaseOverlap(BeamState p1, BeamState p2) {
        java.util.Set<String> s1 = new java.util.HashSet<>();
        for (JejuRoadPoint pt : p1.getRoute()) {
            s1.add(String.format("%.4f,%.4f", pt.getLatitude(), pt.getLongitude()));
        }
        long overlap = p2.getRoute().stream()
            .filter(pt -> s1.contains(String.format("%.4f,%.4f", pt.getLatitude(), pt.getLongitude())))
            .count();
        int minSize = Math.min(p1.getRoute().size(), p2.getRoute().size());
        return minSize == 0 ? 0.0 : (double) overlap / minSize;
    }

    private BeamState combinePhases(BeamState p1, BeamState p2) {
        BeamState combined = p1.clone();
        combined.setPhaseOneBoundary(p1.getRoute().size());
        combined.setTargetPassed(true);

        // Phase2는 앵커에서 출발점으로 이미 올바른 방향 → 뒤집기 불필요
        List<JejuRoadPoint> p2Route = p2.getRoute();

        JejuRoadPoint prev = p1.getCurrentPoint();
        for (JejuRoadPoint pt : p2Route) {
            double d = HaversineUtil.calculate(
                prev.getLatitude(), prev.getLongitude(),
                pt.getLatitude(), pt.getLongitude());
            combined.addPoint(pt, d, 0.0);
            prev = pt;
        }
        combined.setCompleted(true);
        return combined;
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
        double estimatedTotal = beam.getTraveledDistance() + distToStart;
        return Math.abs(estimatedTotal - targetDist) <= tolerance;
    }

    private double[] generateAnchorPoint(double startLat, double startLng, double targetDistance) {
        double anchorDist = targetDistance * 0.38;
        double minAnchorDist = anchorDist * 0.85;
        double maxAnchorDist = anchorDist * 1.15;
        double cosLat = Math.cos(Math.toRadians(startLat));

        // 16방향으로 탐색 + pointId 기준 중복 제거 → 다양한 앵커 풀 확보
        java.util.Map<Integer, JejuRoadPoint> candidateMap = new java.util.LinkedHashMap<>();
        for (double searchRadius : new double[]{600.0, 1200.0, 2000.0}) {
            for (int i = 0; i < 16; i++) {
                double rad = Math.toRadians(360.0 / 16 * i);
                double dirLat = startLat + (anchorDist / 111000.0) * Math.cos(rad);
                double dirLng = startLng + (anchorDist / (111000.0 * cosLat)) * Math.sin(rad);

                cache.findRoadPointsWithinRadius(dirLat, dirLng, searchRadius).stream()
                    .filter(p -> {
                        double d = HaversineUtil.calculate(startLat, startLng, p.getLatitude(), p.getLongitude());
                        return d >= minAnchorDist && d <= maxAnchorDist;
                    })
                    .forEach(p -> candidateMap.putIfAbsent(p.getPointId(), p));
            }
            if (!candidateMap.isEmpty()) break;
        }

        java.util.List<JejuRoadPoint> candidates = new java.util.ArrayList<>(candidateMap.values());
        if (!candidates.isEmpty()) {
            int idx = (int) (Math.random() * candidates.size());
            JejuRoadPoint chosen = candidates.get(idx);
            double chosenDist = HaversineUtil.calculate(startLat, startLng, chosen.getLatitude(), chosen.getLongitude());
            log.info("│ 앵커 선택: {}개 후보 중 {}번째 — 거리 {}m ({}, {})",
                candidates.size(), idx, (int) chosenDist,
                String.format("%.5f", chosen.getLatitude()),
                String.format("%.5f", chosen.getLongitude()));
            return new double[]{chosen.getLatitude(), chosen.getLongitude()};
        }

        // 최후 폴백: 수학적 북쪽 좌표 (도로 없어도 거리는 보장)
        log.warn("│ ⚠ 앵커 후보 없음 — 수학적 북쪽 사용 (거리 {}m)", (int) anchorDist);
        return new double[]{startLat + anchorDist / 111000.0, startLng};
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

