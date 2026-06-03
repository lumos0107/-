/*
 * DirectionSplitBeamSearchStrategy
 * - 초기 빔마다 서로 다른 진행 방향을 부여해 경로가 같은 방향으로 수렴하는 현상을 줄인다.
 * - 후보 이동 방향과 다른 빔 경로 중복도에 패널티를 주는 1번 개선 알고리즘이다.
 */
package com.gildongmu.algorithm.direction;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.CandidateScore;
import com.gildongmu.algorithm.HaversineUtil;
import com.gildongmu.algorithm.common.BeamSearchContext;
import com.gildongmu.algorithm.common.BeamSearchStrategy;
import com.gildongmu.database.entity.JejuRoadPoint;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DirectionSplitBeamSearchStrategy implements BeamSearchStrategy {

    private static final int INTERNAL_BEAM_WIDTH = 9;
    private static final int FINAL_ROUTE_COUNT = 3;
    private static final int CANDIDATES_PER_BEAM = 3;
    private static final double QUALITY_WEIGHT = 0.55;
    private static final double DIVERSITY_WEIGHT = 0.45;
    private static final double DIRECTION_PENALTY_WEIGHT = 0.55;
    private static final double CROSS_BEAM_DUPLICATE_WEIGHT = 0.35;
    private static final double CROSS_BEAM_DUPLICATE_RADIUS = 20.0;
    private static final double SELECTED_CANDIDATE_DISTANCE = 20.0;
    private static final double MID_SELECTED_CANDIDATE_DISTANCE = 60.0;
    private static final double DUPLICATE_ROUTE_MIDDLE_OVERLAP = 0.85;
    private static final double STRICT_WHOLE_OVERLAP_LIMIT = 0.95;
    private static final double RELAXED_MIDDLE_OVERLAP_LIMIT = 0.95;
    private static final double RELAXED_WHOLE_OVERLAP_LIMIT = 0.98;
    private static final double DIRECTION_SPLIT_PROGRESS_LIMIT = 0.55;
    private static final double DUPLICATE_PENALTY_START_RATIO = 0.2;
    private static final double DUPLICATE_PENALTY_END_RATIO = 0.75;
    private static final double[] DIRECTION_OFFSETS = {
        -45.0, -34.0, -23.0, -12.0, 0.0, 12.0, 23.0, 34.0, 45.0
    };

    @Override
    public String getName() {
        return "DIRECTION_SPLIT";
    }

    @Override
    public int getBeamWidth(BeamSearchContext context) {
        return INTERNAL_BEAM_WIDTH;
    }

    @Override
    public List<BeamState> initializeBeams(JejuRoadPoint startPoint, BeamSearchContext context) {
        List<BeamState> beams = new ArrayList<>();

        for (int i = 0; i < INTERNAL_BEAM_WIDTH; i++) {
            BeamState beam = new BeamState();
            beam.setBeamIndex(i);
            double offset = DIRECTION_OFFSETS[i % DIRECTION_OFFSETS.length];
            beam.setDirectionOffset(offset);
            beam.setPreferredBearing(normalizeBearing(calculateBearing(
                context.getStartLat(), context.getStartLng(),
                context.getTargetLat(), context.getTargetLng()
            ) + offset));
            beam.addPoint(startPoint, 0.0, 0.0);
            beams.add(beam);
        }
        return beams;
    }

    @Override
    public double adjustScore(
        double baseScore,
        BeamState sourceBeam,
        JejuRoadPoint candidate,
        List<BeamState> currentBeams,
        BeamSearchContext context
    ) {
        double adjusted = baseScore;

        double progressRatio = sourceBeam.getTraveledDistance() / context.getTargetDistance();
        if (sourceBeam.getDirectionOffset() != null
            && progressRatio < DIRECTION_SPLIT_PROGRESS_LIMIT) {
            JejuRoadPoint current = sourceBeam.getCurrentPoint();
            double guideLat = context.getTargetLat();
            double guideLng = context.getTargetLng();
            double guideBearing = calculateBearing(
                current.getLatitude(), current.getLongitude(),
                guideLat, guideLng
            );
            double desiredBearing = normalizeBearing(guideBearing + sourceBeam.getDirectionOffset());
            double candidateBearing = calculateBearing(
                current.getLatitude(), current.getLongitude(),
                candidate.getLatitude(), candidate.getLongitude()
            );
            double angleDiff = bearingDifference(desiredBearing, candidateBearing);
            adjusted -= DIRECTION_PENALTY_WEIGHT * (angleDiff / 180.0);
        }

        if (progressRatio >= DUPLICATE_PENALTY_START_RATIO
            && progressRatio <= DUPLICATE_PENALTY_END_RATIO) {
            double duplicatePenalty = calculateCrossBeamDuplicatePenalty(sourceBeam, candidate, currentBeams);
            adjusted -= CROSS_BEAM_DUPLICATE_WEIGHT * duplicatePenalty;
        }
        return adjusted;
    }

    @Override
    public List<CandidateScore> selectCandidates(
        List<CandidateScore> candidates,
        int limit,
        BeamSearchContext context
    ) {
        Map<BeamState, List<CandidateScore>> candidatesByBeam = new LinkedHashMap<>();

        candidates.stream()
            .sorted(Comparator.comparingDouble(CandidateScore::getScore).reversed())
            .forEach(candidate -> {
                List<CandidateScore> beamCandidates = candidatesByBeam.computeIfAbsent(
                    candidate.getSourceBeam(), key -> new ArrayList<>()
                );
                if (beamCandidates.size() < CANDIDATES_PER_BEAM) {
                    beamCandidates.add(candidate);
                }
            });

        List<CandidateScore> selected = new ArrayList<>();
        int maxRounds = candidatesByBeam.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(0);

        for (int round = 0; round < maxRounds && selected.size() < limit; round++) {
            List<CandidateScore> roundCandidates = new ArrayList<>();
            for (List<CandidateScore> beamCandidates : candidatesByBeam.values()) {
                if (beamCandidates.size() > round) {
                    roundCandidates.add(beamCandidates.get(round));
                }
            }

            roundCandidates.sort(Comparator.comparingInt(candidate -> {
                Integer beamIndex = candidate.getSourceBeam().getBeamIndex();
                return beamIndex != null ? beamIndex : Integer.MAX_VALUE;
            }));

            for (CandidateScore candidate : roundCandidates) {
                if (selected.size() >= limit) {
                    break;
                }
            if (isTooCloseToSelectedCandidate(candidate, selected, context)) {
                continue;
            }
                selected.add(candidate);
            }
        }

        if (selected.size() < limit) {
            List<CandidateScore> ranked = candidatesByBeam.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparingDouble(CandidateScore::getScore).reversed())
                .toList();
            for (CandidateScore candidate : ranked) {
                if (selected.size() >= limit) {
                    break;
                }
                if (!selected.contains(candidate)) {
                    selected.add(candidate);
                }
            }
        }

        return selected;
    }

    @Override
    public List<BeamState> selectFinalRoutes(List<BeamState> routes, BeamSearchContext context) {
        List<BeamState> remaining = new ArrayList<>(routes);
        List<BeamState> selected = new ArrayList<>();

        selectRoutesByOverlapLimit(
            remaining, selected,
            DUPLICATE_ROUTE_MIDDLE_OVERLAP,
            STRICT_WHOLE_OVERLAP_LIMIT
        );
        selectRoutesByOverlapLimit(
            remaining, selected,
            RELAXED_MIDDLE_OVERLAP_LIMIT,
            RELAXED_WHOLE_OVERLAP_LIMIT
        );
        selectRoutesWithoutOverlapLimit(remaining, selected);

        return selected;
    }

    private void selectRoutesByOverlapLimit(
        List<BeamState> remaining,
        List<BeamState> selected,
        double middleLimit,
        double wholeLimit
    ) {
        while (!remaining.isEmpty() && selected.size() < FINAL_ROUTE_COUNT) {
            BeamState best = remaining.stream()
                .filter(route -> selected.stream().noneMatch(selectedRoute ->
                    middleOverlap(route, selectedRoute) >= middleLimit
                        || wholeOverlap(route, selectedRoute) >= wholeLimit
                ))
                .max(Comparator.comparingDouble(route -> finalSelectionScore(route, remaining, selected)))
                .orElse(null);

            if (best == null) {
                break;
            }

            selected.add(best);
            remaining.remove(best);
        }
    }

    private void selectRoutesWithoutOverlapLimit(
        List<BeamState> remaining,
        List<BeamState> selected
    ) {
        while (!remaining.isEmpty() && selected.size() < FINAL_ROUTE_COUNT) {
            BeamState best = remaining.stream()
                .max(Comparator.comparingDouble(route -> finalSelectionScore(route, remaining, selected)))
                .orElseThrow();
            selected.add(best);
            remaining.remove(best);
        }
    }

    private double finalSelectionScore(
        BeamState route,
        List<BeamState> candidates,
        List<BeamState> selected
    ) {
        double quality = normalizeQuality(route, candidates);
        double diversity = selected.isEmpty()
            ? 1.0
            : 1.0 - selected.stream()
                .mapToDouble(selectedRoute -> middleOverlap(route, selectedRoute))
                .max()
                .orElse(0.0);

        return QUALITY_WEIGHT * quality + DIVERSITY_WEIGHT * diversity;
    }

    private double normalizeQuality(BeamState route, List<BeamState> candidates) {
        double min = candidates.stream()
            .mapToDouble(BeamState::getTotalScore)
            .min()
            .orElse(route.getTotalScore());
        double max = candidates.stream()
            .mapToDouble(BeamState::getTotalScore)
            .max()
            .orElse(route.getTotalScore());

        if (Math.abs(max - min) < 0.000001) {
            return 1.0;
        }
        return (route.getTotalScore() - min) / (max - min);
    }

    private boolean isTooCloseToSelectedCandidate(
        CandidateScore candidate,
        List<CandidateScore> selected,
        BeamSearchContext context
    ) {
        double progressRatio = candidate.getSourceBeam().getTraveledDistance() / context.getTargetDistance();
        double distanceThreshold = progressRatio >= DUPLICATE_PENALTY_START_RATIO
            && progressRatio <= DUPLICATE_PENALTY_END_RATIO
            ? MID_SELECTED_CANDIDATE_DISTANCE
            : SELECTED_CANDIDATE_DISTANCE;

        return selected.stream().anyMatch(selectedCandidate -> {
            double distance = HaversineUtil.calculate(
                candidate.getCandidate().getLatitude(),
                candidate.getCandidate().getLongitude(),
                selectedCandidate.getCandidate().getLatitude(),
                selectedCandidate.getCandidate().getLongitude()
            );
            return distance <= distanceThreshold;
        });
    }

    private double middleOverlap(BeamState a, BeamState b) {
        Set<String> aPoints = toMiddleRoundedPointSet(a);
        Set<String> bPoints = toMiddleRoundedPointSet(b);
        if (aPoints.isEmpty() || bPoints.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(aPoints);
        intersection.retainAll(bPoints);
        return intersection.size() / (double) Math.min(aPoints.size(), bPoints.size());
    }

    private double wholeOverlap(BeamState a, BeamState b) {
        Set<String> aPoints = toRoundedPointSet(a);
        Set<String> bPoints = toRoundedPointSet(b);
        if (aPoints.isEmpty() || bPoints.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(aPoints);
        intersection.retainAll(bPoints);
        return intersection.size() / (double) Math.min(aPoints.size(), bPoints.size());
    }

    private Set<String> toRoundedPointSet(BeamState route) {
        Set<String> points = new HashSet<>();
        for (JejuRoadPoint point : route.getRoute()) {
            points.add(String.format("%.5f,%.5f", point.getLatitude(), point.getLongitude()));
        }
        return points;
    }

    private Set<String> toMiddleRoundedPointSet(BeamState route) {
        List<JejuRoadPoint> points = route.getRoute();
        int from = (int) Math.floor(points.size() * 0.2);
        int to = (int) Math.ceil(points.size() * 0.8);

        Set<String> result = new HashSet<>();
        for (JejuRoadPoint point : points.subList(from, to)) {
            result.add(String.format("%.5f,%.5f", point.getLatitude(), point.getLongitude()));
        }
        return result;
    }

    private double calculateCrossBeamDuplicatePenalty(
        BeamState sourceBeam,
        JejuRoadPoint candidate,
        List<BeamState> currentBeams
    ) {
        int duplicateCount = 0;
        for (BeamState beam : currentBeams) {
            if (beam == sourceBeam) {
                continue;
            }
            for (double[] visited : beam.getVisitedPoints()) {
                double distance = HaversineUtil.calculate(
                    candidate.getLatitude(), candidate.getLongitude(),
                    visited[0], visited[1]
                );
                if (distance <= CROSS_BEAM_DUPLICATE_RADIUS) {
                    duplicateCount++;
                    break;
                }
            }
        }
        return duplicateCount / Math.max(1.0, currentBeams.size() - 1.0);
    }

    private double calculateBearing(double fromLat, double fromLng, double toLat, double toLng) {
        double lat1 = Math.toRadians(fromLat);
        double lat2 = Math.toRadians(toLat);
        double deltaLng = Math.toRadians(toLng - fromLng);
        double y = Math.sin(deltaLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
            - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLng);
        return normalizeBearing(Math.toDegrees(Math.atan2(y, x)));
    }

    private double normalizeBearing(double bearing) {
        return (bearing + 360.0) % 360.0;
    }

    private double bearingDifference(double a, double b) {
        double diff = Math.abs(normalizeBearing(a) - normalizeBearing(b));
        return Math.min(diff, 360.0 - diff);
    }
}

