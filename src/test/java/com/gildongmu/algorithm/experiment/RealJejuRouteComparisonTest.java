/*
 * RealJejuRouteComparisonTest
 * - RDS의 실제 제주 도로 데이터를 사용해 기본 알고리즘과 1번 방향분리 알고리즘 결과를 비교한다.
 * - DB_PASSWORD가 있는 환경에서만 실행되는 알고리즘 담당자용 통합 테스트다.
 */
package com.gildongmu.algorithm.experiment;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.basic.BeamSearchAlgorithm;
import com.gildongmu.algorithm.direction.DirectionSplitBeamSearchAlgorithm;
import com.gildongmu.algorithm.macro.MacroDirectionBeamSearchAlgorithm;
import com.gildongmu.database.entity.JejuRoadPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class RealJejuRouteComparisonTest {

    private static final double SEGMENT_LENGTH_METERS = 500.0;

    @Autowired
    private BeamSearchAlgorithm basicAlgorithm;

    @Autowired
    private DirectionSplitBeamSearchAlgorithm directionAlgorithm;

    @Autowired
    private MacroDirectionBeamSearchAlgorithm macroDirectionAlgorithm;

    @Test
    void compareBasicAndDirectionSplitWithRealJejuData() {
        double startLat = 33.499;
        double startLng = 126.531;
        double targetDistance = 3000.0;

        List<BeamState> basic = basicAlgorithm.generate(startLat, startLng, targetDistance, null, null);
        List<BeamState> direction = directionAlgorithm.generate(startLat, startLng, targetDistance, null, null);
        List<BeamState> macroDirection = macroDirectionAlgorithm.generate(startLat, startLng, targetDistance, null, null);

        printSummary("BASIC", basic);
        printSummary("DIRECTION_SPLIT", direction);
        printSummary("MACRO_DIRECTION", macroDirection);
    }

    private void printSummary(String label, List<BeamState> routes) {
        System.out.println("==== " + label + " ====");
        System.out.println("routeCount=" + routes.size());

        for (int i = 0; i < routes.size(); i++) {
            BeamState route = routes.get(i);
            System.out.println(String.format(
                "route[%d] points=%d distance=%d score=%.4f signature=%s",
                i,
                route.getRoute().size(),
                (int) route.getTraveledDistance(),
                route.getTotalScore(),
                signature(route)
            ));
        }

        if (!routes.isEmpty()) {
            System.out.println("route[0].fullPoints=" + fullPoints(routes.get(0)));
        }

        for (int i = 0; i < routes.size(); i++) {
            for (int j = i + 1; j < routes.size(); j++) {
                System.out.println(String.format(
                    "sameSequence[%d,%d]=%s wholeOverlap[%d,%d]=%.4f middleOverlap[%d,%d]=%.4f",
                    i, j,
                    sameSequence(routes.get(i), routes.get(j)),
                    i, j,
                    overlap(routes.get(i), routes.get(j)),
                    i, j,
                    middleOverlap(routes.get(i), routes.get(j))
                ));
                printSegmentOverlaps(i, j, routes.get(i), routes.get(j));
            }
        }
    }

    private String signature(BeamState route) {
        return route.getRoute().stream()
            .limit(8)
            .map(p -> String.format("%.5f,%.5f", p.getLatitude(), p.getLongitude()))
            .collect(Collectors.joining(" | "));
    }

    private String fullPoints(BeamState route) {
        return route.getRoute().stream()
            .map(p -> String.format("%.5f,%.5f", p.getLatitude(), p.getLongitude()))
            .collect(Collectors.joining(" -> "));
    }

    private double overlap(BeamState a, BeamState b) {
        Set<String> aPoints = toRoundedPointSet(a);
        Set<String> bPoints = toRoundedPointSet(b);
        if (aPoints.isEmpty() || bPoints.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(aPoints);
        intersection.retainAll(bPoints);
        return intersection.size() / (double) Math.min(aPoints.size(), bPoints.size());
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

    private void printSegmentOverlaps(int leftIndex, int rightIndex, BeamState left, BeamState right) {
        double maxDistance = Math.min(left.getTraveledDistance(), right.getTraveledDistance());
        int segmentCount = (int) Math.ceil(maxDistance / SEGMENT_LENGTH_METERS);

        for (int segment = 0; segment < segmentCount; segment++) {
            double from = segment * SEGMENT_LENGTH_METERS;
            double to = Math.min((segment + 1) * SEGMENT_LENGTH_METERS, maxDistance);
            double overlap = segmentOverlap(left, right, from, to);
            System.out.println(String.format(
                "segmentOverlap[%d,%d][%.0f-%.0fm]=%.4f",
                leftIndex, rightIndex, from, to, overlap
            ));
        }
    }

    private double segmentOverlap(BeamState a, BeamState b, double fromMeters, double toMeters) {
        Set<String> aPoints = toSegmentRoundedPointSet(a, fromMeters, toMeters);
        Set<String> bPoints = toSegmentRoundedPointSet(b, fromMeters, toMeters);
        if (aPoints.isEmpty() || bPoints.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(aPoints);
        intersection.retainAll(bPoints);
        return intersection.size() / (double) Math.min(aPoints.size(), bPoints.size());
    }

    private boolean sameSequence(BeamState a, BeamState b) {
        List<JejuRoadPoint> aRoute = a.getRoute();
        List<JejuRoadPoint> bRoute = b.getRoute();
        if (aRoute.size() != bRoute.size()) {
            return false;
        }

        for (int i = 0; i < aRoute.size(); i++) {
            if (!roundedPoint(aRoute.get(i)).equals(roundedPoint(bRoute.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private Set<String> toRoundedPointSet(BeamState route) {
        return route.getRoute().stream()
            .map(this::roundedPoint)
            .collect(Collectors.toSet());
    }

    private Set<String> toMiddleRoundedPointSet(BeamState route) {
        List<JejuRoadPoint> points = route.getRoute();
        int from = (int) Math.floor(points.size() * 0.2);
        int to = (int) Math.ceil(points.size() * 0.8);
        return points.subList(from, to).stream()
            .map(this::roundedPoint)
            .collect(Collectors.toSet());
    }

    private Set<String> toSegmentRoundedPointSet(BeamState route, double fromMeters, double toMeters) {
        List<JejuRoadPoint> points = route.getRoute();
        Set<String> result = new HashSet<>();
        double cumulative = 0.0;

        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                JejuRoadPoint prev = points.get(i - 1);
                JejuRoadPoint current = points.get(i);
                cumulative += com.gildongmu.algorithm.HaversineUtil.calculate(
                    prev.getLatitude(), prev.getLongitude(),
                    current.getLatitude(), current.getLongitude()
                );
            }

            if (cumulative >= fromMeters && cumulative < toMeters) {
                result.add(roundedPoint(points.get(i)));
            }
        }

        return result;
    }

    private String roundedPoint(JejuRoadPoint point) {
        return String.format("%.5f,%.5f", point.getLatitude(), point.getLongitude());
    }
}
