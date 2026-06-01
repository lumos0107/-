export function metersToKm(meters: number): string {
  return (meters / 1000).toFixed(1)
}

export function secondsToMinutes(seconds: number): number {
  return Math.floor(seconds / 60)
}

export function secondsToPace(secondsPerKm: number): string {
  const min = Math.floor(secondsPerKm / 60)
  const sec = secondsPerKm % 60
  return `${min}'${String(sec).padStart(2, '0')}"`
}

export function secondsToTime(totalSeconds: number): string {
  const min = Math.floor(totalSeconds / 60)
  const sec = totalSeconds % 60
  return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}
