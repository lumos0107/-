/*
 * MacroDirectionBeamSearchAlgorithm
 * - 500m 거점 단위로 방향을 나누고, 거점 사이를 도로 포인트 리스트로 채우는 1번 개선 알고리즘 v3이다.
 * - 초반에는 앵커를 향해 여러 방향으로 벌리고, 앵커 통과 후에는 출발점으로 귀환해 루프를 완성한다.
 */
package com.gildongmu.algorithm.macro;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.GeoDataCache;
import com.gildongmu.algorithm.HaversineUtil;
import com.gildongmu.database.entity.JejuRoadPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MacroDirectionBeamSearchAlgorithm {

    private static final int INTERNAL_BEAM_WIDTH = 12;
    private static final int FINAL_ROUTE_COUNT = 3;
    private static final int MAX_MACRO_ITERATIONS = 8;
    private static final int PRESELECT_CANDIDATES_PER_BEAM = 8;
    private static final double MACRO_STEP_METERS = 430.0;
    private static final double MACRO_MIN_METERS = 300.0;
    private static final double MACRO_MAX_METERS = 560.0;
    private static final double EARLY_SPLIT_DISTANCE_METERS = 1000.0;
    private static final double EARLY_WAYPOINT_MIN_DISTANCE = 250.0;
    private static final double NORMAL_WAYPOINT_MIN_DISTANCE = 180.0;
    private static final double LOCAL_STEP_RADIUS = 90.0;
    private static final double ANCHOR_PASS_RADIUS = 120.0;
    private static final double LOOP_CLOSE_RADIUS = 140.0;
    private static final double TOLERANCE_RATIO = 0.08;
    private static final double FINAL_TOLERANCE_RATIO = 0.20;
    private static final double[] DIRECTION_OFFSETS = {
        -70.0, -55.0, -40.0, -25.0, -10.0, 0.0, 10.0, 25.0, 40.0, 55.0, 70.0, 85.0
    };

    private final GeoDataCache cache;

    public List<BeamState> generate(double startLat, double startLng,
                                    double targetDistance,
                                    Double waypointLat, Double waypointLng) {
        double tolerance = targetDistance * TOLERANCE_RATIO;
        double[] anchor = waypointLat != null
            ? new double[]{waypointLat, waypointLng}
            : generateAnchorPoint(startLat, startLng, targetDistance);

        JejuRoadPoint startPoint = cache.findNearestRoadPoint(startLat, startLng)
            .orElseThrow(() -> new RuntimeException("출발점 근처 도로 없음"));

        List<MacroRouteState> beams = initializeBeams(startPoint);

        for (int iteration = 0; iteration < MAX_MACRO_ITERATIONS; iteration++) {
            List<MacroRouteState> completed = beams.stream()
                .filter(MacroRouteState::isCompleted)
                .collect(Collectors.toCollection(ArrayList::new));

            List<MacroWaypointCandidate> candidates = new ArrayList<>();
            for (MacroRouteState beam : beams) {
                if (!beam.isCompleted()) {
                    candidates.addAll(buildCandidates(beam, startPoint, anchor, targetDistance, tolerance));
                }
            }

            if (candidates.isEmpty()) {
                break;
            }

            List<MacroRouteState> next = new ArrayList<>(completed);
            for (MacroWaypointCandidate candidate : selectCandidates(candidates, INTERNAL_BEAM_WIDTH - completed.size())) {
                MacroRouteState state = candidate.getSource().copy();
                state.addPath(candidate.getPath(), candidate.getDistance(), candidate.getScore());

                JejuRoadPoint current = state.getCurrentPoint();
                double distToAnchor = HaversineUtil.calculate(
                    current.getLatitude(), current.getLongitude(), anchor[0], anchor[1]
                );
                if (distToAnchor <= ANCHOR_PASS_RADIUS || state.getTraveledDistance() >= targetDistance * 0.45) {
                    state.setTargetPassed(true);
                }

                double distToStart = HaversineUtil.calculate(
                    current.getLatitude(), current.getLongitude(),
                    startPoint.getLatitude(), startPoint.getLongitude()
                );
                double totalWithReturn = state.getTraveledDistance() + distToStart;
                if (state.isTargetPassed()
                    && distToStart <= LOOP_CLOSE_RADIUS
                    && Math.abs(state.getTraveledDistance() - targetDistance) <= tolerance) {
                    state.setCompleted(true);
                } else if (state.isTargetPassed()
                    && Math.abs(totalWithReturn - targetDistance) <= tolerance
                    && distToStart <= MACRO_MAX_METERS) {
                    List<JejuRoadPoint> returnPath = connectLocalPath(current, startPoint);
                    double returnDistance = calculatePathDistance(returnPath);
                    state.addPath(returnPath, returnDistance, 1.0);
                    state.setCompleted(true);
                }

                next.add(state);
            }

            beams = next.stream()
                .sorted(Comparator.comparingDouble(MacroRouteState::getTotalScore).reversed())
                .limit(INTERNAL_BEAM_WIDTH)
                .collect(Collectors.toList());
        }

        List<MacroRouteState> closed = closeFinalStates(beams, startPoint, targetDistance);

        List<MacroRouteState> valid = closed.stream()
            .filter(state -> isValidLoop(state, startPoint, targetDistance, tolerance))
            .collect(Collectors.toList());

        return selectFinalRoutes(valid, targetDistance).stream()
            .map(this::toBeamState)
            .collect(Collectors.toList());
    }

    private List<MacroRouteState> initializeBeams(JejuRoadPoint startPoint) {
        List<MacroRouteState> beams = new ArrayList<>();
        for (int i = 0; i < INTERNAL_BEAM_WIDTH; i++) {
            MacroRouteState state = new MacroRouteState(i, DIRECTION_OFFSETS[i % DIRECTION_OFFSETS.length]);
            state.addInitialPoint(startPoint);
            beams.add(state);
        }
        return beams;
    }

    private List<MacroWaypointCandidate> buildCandidates(MacroRouteState state,
                                                         JejuRoadPoint startPoint,
                                                         double[] anchor,
                                                         double targetDistance,
                                                         double tolerance) {
        JejuRoadPoint current = state.getCurrentPoint();
        double curLat = current.getLatitude();
        double curLng = current.getLongitude();

        List<JejuRoadPoint> waypoints = cache.findRoadPointsWithinRadius(curLat, curLng, MACRO_MAX_METERS).stream()
            .filter(waypoint -> {
                double directDistance = HaversineUtil.calculate(curLat, curLng, waypoint.getLatitude(), waypoint.getLongitude());
                if (directDistance < MACRO_MIN_METERS || directDistance > MACRO_MAX_METERS) {
                    return false;
                }

                double remainingToStart = HaversineUtil.calculate(
                    waypoint.getLatitude(), waypoint.getLongitude(),
                    startPoint.getLatitude(), startPoint.getLongitude()
                );
                return state.getTraveledDistance() + directDistance + remainingToStart <= targetDistance + tolerance;
            })
            .sorted(Comparator.comparingDouble(waypoint ->
                -scoreWaypoint(
                    state,
                    waypoint,
                    startPoint,
                    anchor,
                    targetDistance,
                    HaversineUtil.calculate(curLat, curLng, waypoint.getLatitude(), waypoint.getLongitude())
                )
            ))
            .limit(PRESELECT_CANDIDATES_PER_BEAM)
            .collect(Collectors.toList());

        List<MacroWaypointCandidate> candidates = new ArrayList<>();

        for (JejuRoadPoint waypoint : waypoints) {
            double directDistance = HaversineUtil.calculate(curLat, curLng, waypoint.getLatitude(), waypoint.getLongitude());
            List<JejuRoadPoint> localPath = connectLocalPath(current, waypoint);
            if (localPath.size() < 2) {
                continue;
            }

            double pathDistance = calculatePathDistance(localPath);
            double score = scoreWaypoint(state, waypoint, startPoint, anchor, targetDistance, directDistance);
            candidates.add(new MacroWaypointCandidate(state, waypoint, localPath, pathDistance, score));
        }

        return candidates;
    }

    private double scoreWaypoint(MacroRouteState state,
                                 JejuRoadPoint waypoint,
                                 JejuRoadPoint startPoint,
                                 double[] anchor,
                                 double targetDistance,
                                 double directDistance) {
        JejuRoadPoint current = state.getCurrentPoint();
        double guideLat = state.isTargetPassed() ? startPoint.getLatitude() : anchor[0];
        double guideLng = state.isTargetPassed() ? startPoint.getLongitude() : anchor[1];
        double guideBearing = calculateBearing(current.getLatitude(), current.getLongitude(), guideLat, guideLng);
        double desiredBearing = normalizeBearing(guideBearing + state.getDirectionOffset());
        double candidateBearing = calculateBearing(
            current.getLatitude(), current.getLongitude(),
            waypoint.getLatitude(), waypoint.getLongitude()
        );

        double directionScore = 1.0 - bearingDifference(desiredBearing, candidateBearing) / 180.0;
        double stepScore = 1.0 - Math.min(1.0, Math.abs(directDistance - MACRO_STEP_METERS) / MACRO_STEP_METERS);
        double guideDistance = HaversineUtil.calculate(waypoint.getLatitude(), waypoint.getLongitude(), guideLat, guideLng);
        double guideScore = 1.0 / (1.0 + guideDistance / 500.0);
        double progress = (state.getTraveledDistance() + directDistance) / targetDistance;
        double returnDistance = HaversineUtil.calculate(
            waypoint.getLatitude(), waypoint.getLongitude(),
            startPoint.getLatitude(), startPoint.getLongitude()
        );
        double returnScore = progress < 0.55 ? 0.0 : 1.0 / (1.0 + returnDistance / 500.0);
        double reusePenalty = routeReusePenalty(state, waypoint);

        return directionScore * 2.0
            + stepScore * 1.2
            + guideScore * 1.5
            + returnScore * 2.0
            - reusePenalty * 1.2;
    }

    private List<MacroWaypointCandidate> selectCandidates(List<MacroWaypointCandidate> candidates, int limit) {
        List<MacroWaypointCandidate> selected = new ArrayList<>();
        List<MacroWaypointCandidate> ranked = candidates.stream()
            .sorted(Comparator.comparingDouble(MacroWaypointCandidate::getScore).reversed())
            .collect(Collectors.toList());

        for (MacroWaypointCandidate candidate : ranked) {
            if (selected.size() >= limit) {
                break;
            }
            double waypointMinDistance = candidate.getSource().getTraveledDistance() < EARLY_SPLIT_DISTANCE_METERS
                ? EARLY_WAYPOINT_MIN_DISTANCE
                : NORMAL_WAYPOINT_MIN_DISTANCE;
            boolean tooClose = selected.stream().anyMatch(selectedCandidate ->
                HaversineUtil.calculate(
                    candidate.getWaypoint().getLatitude(), candidate.getWaypoint().getLongitude(),
                    selectedCandidate.getWaypoint().getLatitude(), selectedCandidate.getWaypoint().getLongitude()
                ) <= waypointMinDistance
            );
            if (!tooClose) {
                selected.add(candidate);
            }
        }

        for (MacroWaypointCandidate candidate : ranked) {
            if (selected.size() >= limit) {
                break;
            }
            if (!selected.contains(candidate)) {
                selected.add(candidate);
            }
        }

        return selected;
    }

    private List<JejuRoadPoint> connectLocalPath(JejuRoadPoint from, JejuRoadPoint to) {
        List<JejuRoadPoint> path = new ArrayList<>();
        path.add(from);

        JejuRoadPoint current = from;
        Set<JejuRoadPoint> visited = new HashSet<>();
        visited.add(from);

        for (int i = 0; i < 20; i++) {
            double distToTarget = HaversineUtil.calculate(
                current.getLatitude(), current.getLongitude(),
                to.getLatitude(), to.getLongitude()
            );
            if (distToTarget <= LOCAL_STEP_RADIUS) {
                break;
            }

            List<JejuRoadPoint> nearby = cache.findRoadPointsWithinRadius(
                current.getLatitude(), current.getLongitude(), LOCAL_STEP_RADIUS
            );
            JejuRoadPoint next = nearby.stream()
                .filter(point -> !visited.contains(point))
                .min(Comparator.comparingDouble(point ->
                    HaversineUtil.calculate(point.getLatitude(), point.getLongitude(), to.getLatitude(), to.getLongitude())
                ))
                .orElse(null);

            if (next == null) {
                break;
            }

            path.add(next);
            visited.add(next);
            current = next;
        }

        if (path.get(path.size() - 1) != to) {
            path.add(to);
        }
        return path;
    }

    private List<MacroRouteState> selectFinalRoutes(List<MacroRouteState> routes, double targetDistance) {
        List<MacroRouteState> remaining = new ArrayList<>(routes);
        List<MacroRouteState> selected = new ArrayList<>();

        while (!remaining.isEmpty() && selected.size() < FINAL_ROUTE_COUNT) {
            MacroRouteState best = remaining.stream()
                .max(Comparator.comparingDouble(route -> finalScore(route, remaining, selected, targetDistance)))
                .orElseThrow();
            selected.add(best);
            remaining.remove(best);
        }

        return selected;
    }

    private List<MacroRouteState> closeFinalStates(List<MacroRouteState> states,
                                                   JejuRoadPoint startPoint,
                                                   double targetDistance) {
        List<MacroRouteState> closed = new ArrayList<>();
        double finalTolerance = targetDistance * FINAL_TOLERANCE_RATIO;

        for (MacroRouteState state : states) {
            MacroRouteState copy = state.copy();
            if (copy.isTargetPassed() && !copy.isCompleted()) {
                JejuRoadPoint current = copy.getCurrentPoint();
                List<JejuRoadPoint> returnPath = connectLocalPath(current, startPoint);
                double returnDistance = calculatePathDistance(returnPath);
                if (Math.abs(copy.getTraveledDistance() + returnDistance - targetDistance) <= finalTolerance) {
                    copy.addPath(returnPath, returnDistance, 1.0);
                    copy.setCompleted(true);
                }
            }
            closed.add(copy);
        }

        return closed;
    }

    private double finalScore(MacroRouteState route,
                              List<MacroRouteState> candidates,
                              List<MacroRouteState> selected,
                              double targetDistance) {
        double quality = normalizeQuality(route, candidates);
        double diversity = selected.isEmpty()
            ? 1.0
            : 1.0 - selected.stream()
                .mapToDouble(selectedRoute -> overlap(route.getRoute(), selectedRoute.getRoute()))
                .max()
                .orElse(0.0);
        double distanceFit = Math.max(
            0.0,
            1.0 - Math.abs(route.getTraveledDistance() - targetDistance) / targetDistance
        );
        return quality * 0.45 + diversity * 0.35 + distanceFit * 0.20;
    }

    private BeamState toBeamState(MacroRouteState state) {
        BeamState beam = new BeamState();
        for (JejuRoadPoint point : state.getRoute()) {
            JejuRoadPoint current = beam.getCurrentPoint();
            double distance = current == null ? 0.0 : HaversineUtil.calculate(
                current.getLatitude(), current.getLongitude(),
                point.getLatitude(), point.getLongitude()
            );
            beam.addPoint(point, distance, 0.0);
        }
        beam.setTargetPassed(state.isTargetPassed());
        beam.setCompleted(state.isCompleted());
        return beam;
    }

    private boolean isValidLoop(MacroRouteState state, JejuRoadPoint startPoint, double targetDistance, double tolerance) {
        if (!state.isTargetPassed() || state.getRoute().isEmpty()) {
            return false;
        }
        double distToStart = HaversineUtil.calculate(
            state.getCurrentPoint().getLatitude(), state.getCurrentPoint().getLongitude(),
            startPoint.getLatitude(), startPoint.getLongitude()
        );
        return distToStart <= LOOP_CLOSE_RADIUS
            && Math.abs(state.getTraveledDistance() - targetDistance) <= targetDistance * FINAL_TOLERANCE_RATIO;
    }

    private double calculatePathDistance(List<JejuRoadPoint> path) {
        double distance = 0.0;
        for (int i = 1; i < path.size(); i++) {
            JejuRoadPoint prev = path.get(i - 1);
            JejuRoadPoint current = path.get(i);
            distance += HaversineUtil.calculate(
                prev.getLatitude(), prev.getLongitude(),
                current.getLatitude(), current.getLongitude()
            );
        }
        return distance;
    }

    private double routeReusePenalty(MacroRouteState state, JejuRoadPoint waypoint) {
        long count = state.getRoute().stream()
            .filter(point -> HaversineUtil.calculate(
                point.getLatitude(), point.getLongitude(),
                waypoint.getLatitude(), waypoint.getLongitude()
            ) <= 80.0)
            .count();
        return Math.min(1.0, count / 4.0);
    }

    private double normalizeQuality(MacroRouteState route, List<MacroRouteState> candidates) {
        double min = candidates.stream().mapToDouble(MacroRouteState::getTotalScore).min().orElse(route.getTotalScore());
        double max = candidates.stream().mapToDouble(MacroRouteState::getTotalScore).max().orElse(route.getTotalScore());
        if (Math.abs(max - min) < 0.000001) {
            return 1.0;
        }
        return (route.getTotalScore() - min) / (max - min);
    }

    private double overlap(List<JejuRoadPoint> a, List<JejuRoadPoint> b) {
        Set<String> aPoints = a.stream().map(this::roundedPoint).collect(Collectors.toSet());
        Set<String> bPoints = b.stream().map(this::roundedPoint).collect(Collectors.toSet());
        if (aPoints.isEmpty() || bPoints.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(aPoints);
        intersection.retainAll(bPoints);
        return intersection.size() / (double) Math.min(aPoints.size(), bPoints.size());
    }

    private String roundedPoint(JejuRoadPoint point) {
        return String.format("%.5f,%.5f", point.getLatitude(), point.getLongitude());
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
            if (cache.findRoadPointsWithinRadius(candLat, candLng, 120.0).isEmpty()) {
                continue;
            }
            double score = -cache.findObstaclesWithinRadius(candLat, candLng, 200.0).size();
            if (score > bestScore) {
                bestScore = score;
                bestAnchor = new double[]{candLat, candLng};
            }
        }
        return bestAnchor;
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
