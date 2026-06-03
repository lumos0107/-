/*
 * BeamState
 * - 빔서치 중 하나의 후보 경로 상태를 담는다.
 * - 누적 거리, 방문 좌표, 완료 여부, 전략별 메타데이터를 관리한다.
 */
package com.gildongmu.algorithm;

import com.gildongmu.database.entity.JejuRoadPoint;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BeamState {

    private List<JejuRoadPoint> route;
    private double traveledDistance;
    private List<double[]> visitedPoints;
    private boolean targetPassed;
    private boolean completed;
    private double totalScore;
    private Integer beamIndex;
    private Double preferredBearing;
    private Double directionOffset;
    private double anchorLat;
    private double anchorLng;
    private int phaseOneBoundary = 0;
    private boolean sideB = false; // false=오른쪽(Phase1), true=왼쪽(Phase2 역방향)

    public BeamState() {
        this.route = new ArrayList<>();
        this.traveledDistance = 0.0;
        this.visitedPoints = new ArrayList<>();
        this.targetPassed = false;
        this.completed = false;
        this.totalScore = 0.0;
        this.beamIndex = null;
        this.preferredBearing = null;
        this.directionOffset = null;
    }

    public BeamState clone() {
        BeamState copy = new BeamState();
        copy.route = new ArrayList<>(this.route);
        copy.traveledDistance = this.traveledDistance;
        copy.visitedPoints = new ArrayList<>(this.visitedPoints);
        copy.targetPassed = this.targetPassed;
        copy.completed = this.completed;
        copy.totalScore = this.totalScore;
        copy.beamIndex = this.beamIndex;
        copy.preferredBearing = this.preferredBearing;
        copy.directionOffset = this.directionOffset;
        copy.anchorLat = this.anchorLat;
        copy.anchorLng = this.anchorLng;
        copy.phaseOneBoundary = this.phaseOneBoundary;
        copy.sideB = this.sideB;
        return copy;
    }

    public void addPoint(JejuRoadPoint point, double segmentDist, double score) {
        this.route.add(point);
        this.traveledDistance += segmentDist;
        this.visitedPoints.add(new double[]{point.getLatitude(), point.getLongitude()});
        this.totalScore += score;
    }

    public void setTargetPassed(boolean targetPassed) {
        this.targetPassed = targetPassed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setBeamIndex(Integer beamIndex) {
        this.beamIndex = beamIndex;
    }

    public void setPreferredBearing(Double preferredBearing) {
        this.preferredBearing = preferredBearing;
    }

    public void setDirectionOffset(Double directionOffset) {
        this.directionOffset = directionOffset;
    }

    public void setAnchor(double lat, double lng) {
        this.anchorLat = lat;
        this.anchorLng = lng;
    }

    public void setPhaseOneBoundary(int index) {
        this.phaseOneBoundary = index;
    }

    public void setSideB(boolean sideB) {
        this.sideB = sideB;
    }

    public void addVisitedHistory(List<double[]> history) {
        this.visitedPoints.addAll(history);
    }

    // 목표 거리 초과분을 끝에서부터 노드 제거로 트리밍
    public void trimToDistance(double maxDistance) {
        while (traveledDistance > maxDistance && route.size() > 1) {
            JejuRoadPoint last = route.remove(route.size() - 1);
            if (!visitedPoints.isEmpty()) visitedPoints.remove(visitedPoints.size() - 1);
            JejuRoadPoint prev = route.get(route.size() - 1);
            double lastSeg = HaversineUtil.calculate(
                prev.getLatitude(), prev.getLongitude(),
                last.getLatitude(), last.getLongitude());
            traveledDistance -= lastSeg;
        }
    }

    public JejuRoadPoint getCurrentPoint() {
        return route.isEmpty() ? null : route.get(route.size() - 1);
    }

    public JejuRoadPoint getPrevPoint() {
        return route.size() < 2 ? null : route.get(route.size() - 2);
    }
}

