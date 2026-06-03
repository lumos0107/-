/*
 * DirectionSplitBeamSearchAlgorithm
 * - 1번 개선안인 초기방향분리와 경로 간 중복패널티 알고리즘의 진입점이다.
 * - 공통 BeamSearchEngine에 DirectionSplitBeamSearchStrategy를 주입해 실행한다.
 */
package com.gildongmu.algorithm.direction;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.common.BeamSearchEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DirectionSplitBeamSearchAlgorithm {

    private final BeamSearchEngine engine;
    private final DirectionSplitBeamSearchStrategy directionSplitStrategy;

    public List<BeamState> generate(double startLat, double startLng,
                                     double targetDistance,
                                     Double waypointLat, Double waypointLng,
                                     double slopeWeight, double slopeThreshold) {
        return engine.generate(
            startLat, startLng,
            targetDistance,
            waypointLat, waypointLng,
            directionSplitStrategy,
            slopeWeight, slopeThreshold
        );
    }
}

