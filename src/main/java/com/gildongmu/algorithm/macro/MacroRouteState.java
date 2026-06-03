/*
 * MacroRouteState
 * - 500m 거점 기반 경로 탐색에서 하나의 후보 루프 상태를 담는다.
 * - 선택된 도로 포인트 리스트, 누적 거리, 앵커 통과 여부, 방향 오프셋을 관리한다.
 */
package com.gildongmu.algorithm.macro;

import com.gildongmu.database.entity.JejuRoadPoint;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class MacroRouteState {

    private List<JejuRoadPoint> route;
    private double traveledDistance;
    private double totalScore;
    private boolean targetPassed;
    private boolean completed;
    private int beamIndex;
    private double directionOffset;

    public MacroRouteState(int beamIndex, double directionOffset) {
        this.route = new ArrayList<>();
        this.traveledDistance = 0.0;
        this.totalScore = 0.0;
        this.targetPassed = false;
        this.completed = false;
        this.beamIndex = beamIndex;
        this.directionOffset = directionOffset;
    }

    public MacroRouteState copy() {
        MacroRouteState copy = new MacroRouteState(this.beamIndex, this.directionOffset);
        copy.route = new ArrayList<>(this.route);
        copy.traveledDistance = this.traveledDistance;
        copy.totalScore = this.totalScore;
        copy.targetPassed = this.targetPassed;
        copy.completed = this.completed;
        return copy;
    }

    public void addInitialPoint(JejuRoadPoint point) {
        this.route.add(point);
    }

    public void addPath(List<JejuRoadPoint> path, double distance, double score) {
        for (JejuRoadPoint point : path) {
            if (route.isEmpty() || route.get(route.size() - 1) != point) {
                route.add(point);
            }
        }
        this.traveledDistance += distance;
        this.totalScore += score;
    }

    public void setTargetPassed(boolean targetPassed) {
        this.targetPassed = targetPassed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public JejuRoadPoint getCurrentPoint() {
        return route.isEmpty() ? null : route.get(route.size() - 1);
    }
}
