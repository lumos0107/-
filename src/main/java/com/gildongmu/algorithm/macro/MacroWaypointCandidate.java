/*
 * MacroWaypointCandidate
 * - 현재 거점에서 다음 500m 거점 후보로 이동하는 선택지를 담는다.
 * - 후보 거점, 연결 경로, 이동 거리, 점수를 함께 보관한다.
 */
package com.gildongmu.algorithm.macro;

import com.gildongmu.database.entity.JejuRoadPoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class MacroWaypointCandidate {
    private MacroRouteState source;
    private JejuRoadPoint waypoint;
    private List<JejuRoadPoint> path;
    private double distance;
    private double score;
}
