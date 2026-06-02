import { useState, useRef, useCallback } from 'react'
import * as Location from 'expo-location'
import { RoutePoint, LapPace, RunResult, startRun, updateRun, endRun, getStoredUserId } from '../utils/api'
import { haversineMeters, paceSecPerKm } from '../utils/geo'

export interface UseRunningGPSReturn {
  currentLocation: RoutePoint | null
  trackedPoints: RoutePoint[]
  elapsedSeconds: number
  distanceMeters: number
  currentPaceSecPerKm: number
  lapPaces: LapPace[]
  start: (courseId: number) => Promise<void>
  stop: () => Promise<RunResult>
}

export function useRunningGPS(): UseRunningGPSReturn {
  const [currentLocation, setCurrentLocation] = useState<RoutePoint | null>(null)
  const [trackedPoints, setTrackedPoints] = useState<RoutePoint[]>([])
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const [distanceMeters, setDistanceMeters] = useState(0)
  const [currentPaceSecPerKm, setCurrentPaceSecPerKm] = useState(0)
  const [lapPaces, setLapPaces] = useState<LapPace[]>([])

  const recordIdRef = useRef<number | null>(null)
  const locationSubRef = useRef<Location.LocationSubscription | null>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const startTimeRef = useRef(0)
  const distRef = useRef(0)
  const trackedRef = useRef<RoutePoint[]>([])
  const lapPacesRef = useRef<LapPace[]>([])
  const lapStartTimeRef = useRef(0)
  const lapStartDistRef = useRef(0)

  const start = useCallback(async (courseId: number) => {
    const { status } = await Location.requestForegroundPermissionsAsync()
    if (status !== 'granted') throw new Error('위치 권한이 필요합니다')

    const recordId = await startRun(courseId)
    recordIdRef.current = recordId
    startTimeRef.current = Date.now()
    lapStartTimeRef.current = Date.now()
    lapStartDistRef.current = 0
    distRef.current = 0
    trackedRef.current = []
    lapPacesRef.current = []

    timerRef.current = setInterval(() => {
      setElapsedSeconds(Math.floor((Date.now() - startTimeRef.current) / 1000))
    }, 1000)

    locationSubRef.current = await Location.watchPositionAsync(
      { accuracy: Location.Accuracy.High, timeInterval: 3000, distanceInterval: 5 },
      (loc) => {
        const point: RoutePoint = {
          latitude: loc.coords.latitude,
          longitude: loc.coords.longitude,
        }
        setCurrentLocation(point)

        const prev = trackedRef.current[trackedRef.current.length - 1]
        if (prev) {
          const delta = haversineMeters(prev, point)
          distRef.current += delta
          setDistanceMeters(distRef.current)

          const elapsed = Math.floor((Date.now() - startTimeRef.current) / 1000)
          setCurrentPaceSecPerKm(paceSecPerKm(distRef.current, elapsed))

          const prevKm = Math.floor((distRef.current - delta) / 1000)
          const newKm = Math.floor(distRef.current / 1000)
          if (newKm > prevKm) {
            const lapTime = Math.floor((Date.now() - lapStartTimeRef.current) / 1000)
            const lapDist = distRef.current - lapStartDistRef.current
            const lap: LapPace = { km: newKm, paceSecPerKm: paceSecPerKm(lapDist, lapTime) }
            lapPacesRef.current = [...lapPacesRef.current, lap]
            setLapPaces([...lapPacesRef.current])
            lapStartTimeRef.current = Date.now()
            lapStartDistRef.current = distRef.current
          }
        }

        trackedRef.current = [...trackedRef.current, point]
        setTrackedPoints([...trackedRef.current])

        if (recordIdRef.current !== null) {
          updateRun(recordIdRef.current, point.latitude, point.longitude).catch(() => {})
        }
      },
    )
  }, [])

  const stop = useCallback(async (): Promise<RunResult> => {
    locationSubRef.current?.remove()
    locationSubRef.current = null
    if (timerRef.current) clearInterval(timerRef.current)

    const totalTime = Math.floor((Date.now() - startTimeRef.current) / 1000)
    const totalDist = distRef.current
    const avgPace = paceSecPerKm(totalDist, totalTime)

    if (recordIdRef.current !== null) {
      await endRun(recordIdRef.current, Math.round(totalDist), totalTime, avgPace)
    }

    return {
      totalDistanceMeters: totalDist,
      totalTimeSeconds: totalTime,
      averagePaceSeconds: avgPace,
      lapPaces: lapPacesRef.current,
    }
  }, [])

  return {
    currentLocation,
    trackedPoints,
    elapsedSeconds,
    distanceMeters,
    currentPaceSecPerKm,
    lapPaces,
    start,
    stop,
  }
}
