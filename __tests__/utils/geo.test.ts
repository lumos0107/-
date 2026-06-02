import { haversineMeters, paceSecPerKm } from '../../utils/geo'

describe('haversineMeters', () => {
  it('같은 좌표는 0 반환', () => {
    expect(haversineMeters(
      { latitude: 33.4507, longitude: 126.5707 },
      { latitude: 33.4507, longitude: 126.5707 },
    )).toBe(0)
  })

  it('위도 1도 차이는 약 111km', () => {
    const d = haversineMeters(
      { latitude: 0, longitude: 0 },
      { latitude: 1, longitude: 0 },
    )
    expect(d).toBeGreaterThan(110000)
    expect(d).toBeLessThan(112000)
  })

  it('제주 공항 → 중문 약 30km', () => {
    const d = haversineMeters(
      { latitude: 33.5070, longitude: 126.4930 },
      { latitude: 33.2440, longitude: 126.4120 },
    )
    expect(d).toBeGreaterThan(28000)
    expect(d).toBeLessThan(32000)
  })
})

describe('paceSecPerKm', () => {
  it('거리 0이면 0 반환', () => {
    expect(paceSecPerKm(0, 100)).toBe(0)
  })

  it('1000m 360초 → 360 sec/km (6분/km)', () => {
    expect(paceSecPerKm(1000, 360)).toBe(360)
  })

  it('500m 180초 → 360 sec/km', () => {
    expect(paceSecPerKm(500, 180)).toBe(360)
  })
})
