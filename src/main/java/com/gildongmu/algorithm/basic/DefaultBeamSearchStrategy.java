/*
 * DefaultBeamSearchStrategy
 * - 기존 빔서치와 같은 방식으로 초기 빔을 만들고 점수 상위 후보를 선택한다.
 * - 공통 엔진 추출 이후에도 기존 추천 결과의 호환성을 유지한다.
 */
package com.gildongmu.algorithm.basic;

import com.gildongmu.algorithm.BeamState;
import com.gildongmu.algorithm.CandidateScore;
import com.gildongmu.algorithm.common.BeamSearchContext;
import com.gildongmu.algorithm.common.BeamSearchStrategy;
import com.gildongmu.database.entity.JejuRoadPoint;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DefaultBeamSearchStrategy implements BeamSearchStrategy {

    @Override
    public String getName() {
        return "DEFAULT";
    }

    @Override
    public List<BeamState> initializeBeams(JejuRoadPoint startPoint, BeamSearchContext context) {
        List<BeamState> beams = new ArrayList<>();
        for (int i = 0; i < context.getConfig().getBeamWidth(); i++) {
            BeamState beam = new BeamState();
            beam.setBeamIndex(i);
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
        return baseScore;
    }

    @Override
    public List<CandidateScore> selectCandidates(
        List<CandidateScore> candidates,
        int limit,
        BeamSearchContext context
    ) {
        return candidates.stream()
            .sorted(Comparator.comparingDouble(CandidateScore::getScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
}

