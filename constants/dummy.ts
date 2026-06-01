/** UI 개발용 더미 데이터 — API 연결 시 이 파일의 상수를 API 응답으로 교체 */

// POST /api/auth/login 응답 (AuthResponse.java)
export const DUMMY_USER = {
  userId: 1,
  email: 'test@example.com',
}

// POST /api/routes/recommend 응답 (RouteRecommendResponse.java)
export const DUMMY_ROUTES = {
  anchorLatitude: 33.4990,
  anchorLongitude: 126.4880,
  obstacles: [] as Array<{ latitude: number; longitude: number; type: string }>,
  places: [] as Array<{ latitude: number; longitude: number; name: string; category: string }>,
  routes: [
    {
      courseId: 1,
      courseName: '애월 바당길',
      totalDistanceMeters: 5200,
      estimatedDurationSeconds: 1872,
      averageSlopePercent: 0.0,
      obstacleCount: 0,
      points: [] as Array<{ latitude: number; longitude: number }>,
      candidatePoints: [] as Array<{ latitude: number; longitude: number }>,
      phaseOneBoundary: 0,
      reason: '해변 따라 쭉 펼쳐진 코스. 바람 좋고 경치 좋아서 아침에 뛰기 딱임.',
      score: 95, // API 없음 — UI 전용
    },
    {
      courseId: 2,
      courseName: '협재 둘레길',
      totalDistanceMeters: 4800,
      estimatedDurationSeconds: 1728,
      averageSlopePercent: 0.0,
      obstacleCount: 0,
      points: [],
      candidatePoints: [],
      phaseOneBoundary: 0,
      reason: '해수욕장 옆이라 사람들 많지만 길이 넓어서 뛰기 편함.',
      score: 92,
    },
    {
      courseId: 3,
      courseName: '용두암 해안로',
      totalDistanceMeters: 5400,
      estimatedDurationSeconds: 1944,
      averageSlopePercent: 0.0,
      obstacleCount: 0,
      points: [],
      candidatePoints: [],
      phaseOneBoundary: 0,
      reason: '관광지라 사람 피해야 함. 그래도 뷰가 좋아서 페이스 느려도 기분 좋음.',
      score: 88,
    },
  ],
}

// GET /api/runs/history?userId=1 응답 (RunningRecord.java)
export const DUMMY_HISTORY = [
  {
    recordId: 1,
    courseId: 1,
    courseName: '애월 바당길',
    startedAt: '2026-06-01T07:30:00',
    totalDistanceMeters: 5200,
    totalTimeSeconds: 1680,
    averagePaceSeconds: 322,
    bestPaceSeconds: 298,  // API 미지원 — GPS 연동 후 계산 예정
    calories: 312,         // API 미지원
  },
]

// 구간별 페이스 더미 (API 미지원 — GPS 연동 후 실시간 계산 예정)
export const DUMMY_SEGMENTS = [
  { km: 1, paceSeconds: 315 },
  { km: 2, paceSeconds: 328 },
  { km: 3, paceSeconds: 322 },
  { km: 4, paceSeconds: 335 },
  { km: 5, paceSeconds: 320 },
]

export type RouteOption = typeof DUMMY_ROUTES.routes[0]
export type HistoryRecord = typeof DUMMY_HISTORY[0]
