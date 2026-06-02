const BASE_URL = 'http://43.200.178.203:8080'

export type RoutePoint = { latitude: number; longitude: number }

export type RouteOption = {
  courseId: number
  courseName: string
  totalDistanceMeters: number
  estimatedDurationSeconds: number
  averageSlopePercent: number
  obstacleCount: number
  points: RoutePoint[]
  candidatePoints: RoutePoint[]
  phaseOneBoundary: number
  reason: string
  score?: number
}

export type RecommendResponse = {
  routes: RouteOption[]
  anchorLatitude: number
  anchorLongitude: number
  obstacles: Array<{ latitude: number; longitude: number; type: string }>
  places: Array<{ latitude: number; longitude: number; name: string; category: string }>
}

export async function recommendRoutes(
  latitude: number,
  longitude: number,
  targetDistance: number,
): Promise<RecommendResponse> {
  const res = await fetch(`${BASE_URL}/api/routes/recommend`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ latitude, longitude, targetDistance }),
  })
  if (!res.ok) throw new Error('경로 추천 실패')
  return res.json()
}

export type LapPace = { km: number; paceSecPerKm: number }

export type RunResult = {
  totalDistanceMeters: number
  totalTimeSeconds: number
  averagePaceSeconds: number
  lapPaces: LapPace[]
}

export async function startRun(courseId: number): Promise<number> {
  const res = await fetch(`${BASE_URL}/api/runs/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId: 1, courseId }),
  })
  if (!res.ok) throw new Error('러닝 시작 실패')
  const data = await res.json()
  return data.recordId
}

export async function updateRun(
  recordId: number,
  latitude: number,
  longitude: number,
): Promise<void> {
  await fetch(`${BASE_URL}/api/runs/update`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      recordId,
      latitude,
      longitude,
      recordedAt: new Date().toISOString(),
    }),
  })
}

export async function endRun(
  recordId: number,
  totalDistanceMeters: number,
  totalTimeSeconds: number,
  averagePaceSeconds: number,
): Promise<void> {
  await fetch(`${BASE_URL}/api/runs/end`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      recordId,
      totalDistanceMeters: Math.round(totalDistanceMeters),
      totalTimeSeconds,
      averagePaceSeconds,
    }),
  })
}
