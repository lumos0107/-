/*
 * CandidateScore
 * - 특정 빔에서 다음 도로 포인트로 이동했을 때의 후보 점수를 담는다.
 * - 공통 엔진과 전략 클래스가 후보 선택 단계에서 공유한다.
 */
package com.gildongmu.algorithm;

import com.gildongmu.database.entity.JejuRoadPoint;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CandidateScore {
    private BeamState sourceBeam;
    private JejuRoadPoint candidate;
    private double score;
    private double segmentDistance;
}

