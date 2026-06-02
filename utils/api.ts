import * as SecureStore from 'expo-secure-store'

const BASE_URL = 'http://43.200.178.203:8080'

export async function getStoredUserId(): Promise<number> {
  const val = await SecureStore.getItemAsync('userId')
  return val ? Number(val) : 1
}

export async function loginUser(email: string, password: string): Promise<{ userId: number; email: string }> {
  const res = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.message || '로그인에 실패했습니다.')
  }
  const data = await res.json()
  await SecureStore.setItemAsync('userId', String(data.userId))
  return data
}

export async function signupUser(email: string, password: string): Promise<void> {
  const res = await fetch(`${BASE_URL}/api/auth/signup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!res.ok) {
    const data = await res.json().catch(() => ({}))
    throw new Error(data.message || '회원가입에 실패했습니다.')
  }
}

export type RoutePoint = { latitude: number; longitude: number; elevation?: number }

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
  difficulty: string,
): Promise<RecommendResponse> {
  const res = await fetch(`${BASE_URL}/api/routes/recommend`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ latitude, longitude, targetDistance, difficulty }),
  })
  if (!res.ok) throw new Error('경로 추천 실패')
  return res.json()
}

export type LapPace = { km: number; paceSecPerKm: number }

export type RunHistory = {
  recordId: number
  totalDistanceMeters: number
  totalTimeSeconds: number
  averagePaceSeconds: number
  startedAt: string
  endedAt: string
  courseName: string
}

export async function getRunHistory(): Promise<RunHistory[]> {
  const userId = await getStoredUserId()
  const res = await fetch(`${BASE_URL}/api/runs/history?userId=${userId}`)
  if (!res.ok) return []
  return res.json()
}

export type RunResult = {
  totalDistanceMeters: number
  totalTimeSeconds: number
  averagePaceSeconds: number
  lapPaces: LapPace[]
}

export async function startRun(courseId: number, userId?: number): Promise<number> {
  const uid = userId ?? await getStoredUserId()
  const res = await fetch(`${BASE_URL}/api/runs/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId: uid, courseId }),
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
