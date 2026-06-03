/*
 * DirectionSplitBeamSearchStrategyTest
 * - 1번 개선 알고리즘의 초기 방향 분리와 중복 패널티 동작을 DB 없이 검증한다.
 * - Spring Context를 띄우지 않는 순수 단위 테스트다.
 */
package com.gildongmu.algorithm.direction;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.common.BeamSearchConfig;
import com.gildongmu.algorithm.common.BeamSearchContext;
import com.gildongmu.database.entity.JejuRoadPoint;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectionSplitBeamSearchStrategyTest {

    private final DirectionSplitBeamSearchStrategy strategy = new DirectionSplitBeamSearchStrategy();

    @Test
    void initializeBeamsAssignsDifferentDirectionOffsets() {
        JejuRoadPoint startPoint = point(33.0, 126.0);
        BeamSearchContext context = context();

        List<BeamState> beams = strategy.initializeBeams(startPoint, context);

        assertEquals(9, beams.size());
        assertNotEquals(beams.get(0).getDirectionOffset(), beams.get(1).getDirectionOffset());
        assertNotEquals(beams.get(1).getDirectionOffset(), beams.get(2).getDirectionOffset());
        assertEquals(0, beams.get(0).getBeamIndex());
        assertEquals(1, beams.get(1).getBeamIndex());
        assertEquals(2, beams.get(2).getBeamIndex());
    }

    @Test
    void adjustScorePrefersCandidateNearPreferredDirection() {
        BeamState beam = new BeamState();
        beam.setDirectionOffset(0.0);
        beam.addPoint(point(33.0, 126.0), 1000.0, 0.0);

        double eastScore = strategy.adjustScore(
            10.0, beam, point(33.0, 126.001), List.of(beam), context()
        );
        double westScore = strategy.adjustScore(
            10.0, beam, point(33.0, 125.999), List.of(beam), context()
        );

        assertTrue(eastScore > westScore);
    }

    @Test
    void adjustScorePenalizesCandidateVisitedByOtherBeam() {
        BeamState source = new BeamState();
        source.setDirectionOffset(0.0);
        source.addPoint(point(33.0, 126.0), 1000.0, 0.0);

        BeamState other = new BeamState();
        other.addPoint(point(33.0, 126.0), 0.0, 0.0);
        other.addPoint(point(33.0, 126.001), 100.0, 0.0);

        JejuRoadPoint candidate = point(33.0, 126.001);
        double scoreWithoutDuplicate = strategy.adjustScore(
            10.0, source, candidate, List.of(source), context()
        );
        double scoreWithDuplicate = strategy.adjustScore(
            10.0, source, candidate, List.of(source, other), context()
        );

        assertTrue(scoreWithDuplicate < scoreWithoutDuplicate);
    }

    private BeamSearchContext context() {
        return new BeamSearchContext(
            33.0, 126.0,
            33.0, 126.01,
            3000.0, 150.0,
            new BeamSearchConfig(),
            null  // 단위 테스트 — getNeighbors 미사용
        );
    }

    private JejuRoadPoint point(double latitude, double longitude) {
        JejuRoadPoint point = new JejuRoadPoint();
        set(point, "latitude", latitude);
        set(point, "longitude", longitude);
        set(point, "elevationMeters", 0.0);
        return point;
    }

    private void set(JejuRoadPoint point, String fieldName, Object value) {
        try {
            Field field = JejuRoadPoint.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(point, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("테스트 포인트 생성 실패", e);
        }
    }
}
