import { metersToKm, secondsToPace, secondsToTime, secondsToMinutes } from '../../utils/format'

describe('metersToKm', () => {
  it('5200m → "5.2"', () => expect(metersToKm(5200)).toBe('5.2'))
  it('1000m → "1.0"', () => expect(metersToKm(1000)).toBe('1.0'))
  it('4800m → "4.8"', () => expect(metersToKm(4800)).toBe('4.8'))
})

describe('secondsToPace', () => {
  it('322초 → "5\'22\""', () => expect(secondsToPace(322)).toBe("5'22\""))
  it('360초 → "6\'00\""', () => expect(secondsToPace(360)).toBe("6'00\""))
  it('298초 → "4\'58\""', () => expect(secondsToPace(298)).toBe("4'58\""))
})

describe('secondsToTime', () => {
  it('1680초 → "28:00"', () => expect(secondsToTime(1680)).toBe('28:00'))
  it('90초 → "01:30"', () => expect(secondsToTime(90)).toBe('01:30'))
})

describe('secondsToMinutes', () => {
  it('1680초 → 28', () => expect(secondsToMinutes(1680)).toBe(28))
  it('1800초 → 30', () => expect(secondsToMinutes(1800)).toBe(30))
})
