/*
 * BeamSearchConfig
 * - 빔서치 공통 실행에 필요한 상수와 탐색 반경 설정을 보관한다.
 * - 기본 알고리즘과 다양성 개선 알고리즘이 같은 설정 객체를 재사용한다.
 */
package com.gildongmu.algorithm.common;

import lombok.Getter;

@Getter
public class BeamSearchConfig {
    private final int beamWidth = 3;             // 최종 반환 경로 수
    private final int explorationBeamWidth = 7;  // 탐색 중 유지할 후보 빔 수
    private final int maxIterations = 800;
    private final double toleranceRatio = 0.10;
    private final double stepRadius = 200.0;
    private final double minStepDistance = 1.0;
    private final double obstacleQueryRadius = 100.0;
    private final double placeQueryRadius = 500.0;
    private final double targetPassRadius = 300.0;
    private final double closeToStartRadius = 300.0;
}

