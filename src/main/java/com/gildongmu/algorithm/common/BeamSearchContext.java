/*
 * BeamSearchContext
 * - 빔서치 한 번의 실행 동안 공유되는 출발점, 앵커, 목표 거리 정보를 담는다.
 * - 전략 클래스가 긴 파라미터 목록 없이 실행 조건을 참조하게 해준다.
 */
package com.gildongmu.algorithm.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BeamSearchContext {
    private final double startLat;
    private final double startLng;
    private final double targetLat;
    private final double targetLng;
    private final double targetDistance;
    private final double tolerance;
    private final BeamSearchConfig config;
}

