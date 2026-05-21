/*
 * BeamSearchAlgorithm
 * - 기존 기본 빔서치 진입점이다.
 * - RouteService와의 호환성을 위해 generate 메서드를 유지한다.
 * - 내부 실행은 BeamSearchEngine과 DefaultBeamSearchStrategy에 위임한다.
 */
package com.gildongmu.algorithm.basic;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.common.BeamSearchEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BeamSearchAlgorithm {

    private final BeamSearchEngine engine;
    private final DefaultBeamSearchStrategy defaultStrategy;

    public List<BeamState> generate(double startLat, double startLng,
                                     double targetDistance,
                                     Double waypointLat, Double waypointLng) {
        return engine.generate(
            startLat, startLng,
            targetDistance,
            waypointLat, waypointLng,
            defaultStrategy
        );
    }
}

