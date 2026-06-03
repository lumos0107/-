/*
 * BeamSearchStrategy
 * - 빔서치 공통 엔진에서 알고리즘별로 달라지는 동작을 정의한다.
 * - 초기 빔 생성, 후보 점수 보정, 후보 선택 방식을 전략별로 교체한다.
 */
package com.gildongmu.algorithm.common;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.CandidateScore;
import com.gildongmu.database.entity.JejuRoadPoint;
import java.util.List;

public interface BeamSearchStrategy {
    String getName();

    default int getBeamWidth(BeamSearchContext context) {
        return context.getConfig().getBeamWidth();
    }

    List<BeamState> initializeBeams(JejuRoadPoint startPoint, BeamSearchContext context);

    double adjustScore(
        double baseScore,
        BeamState sourceBeam,
        JejuRoadPoint candidate,
        List<BeamState> currentBeams,
        BeamSearchContext context
    );

    List<CandidateScore> selectCandidates(
        List<CandidateScore> candidates,
        int limit,
        BeamSearchContext context
    );

    default List<BeamState> selectFinalRoutes(List<BeamState> routes, BeamSearchContext context) {
        return routes;
    }
}

