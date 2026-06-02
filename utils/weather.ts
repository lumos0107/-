const OW_KEY = process.env.EXPO_PUBLIC_OPENWEATHER_KEY ?? ''

export type WeatherData = {
  temp: number
  condition: string
  humidity: number
  windSpeed: number
  icon: string
}

export async function fetchWeather(latitude: number, longitude: number): Promise<WeatherData> {
  const url = `https://api.openweathermap.org/data/2.5/weather?lat=${latitude}&lon=${longitude}&appid=${OW_KEY}&units=metric&lang=kr`
  const res = await fetch(url)
  if (!res.ok) throw new Error('날씨 정보를 가져오지 못했습니다.')
  const data = await res.json()
  return {
    temp: Math.round(data.main.temp),
    condition: data.weather[0].description,
    humidity: data.main.humidity,
    windSpeed: data.wind.speed,
    icon: data.weather[0].icon,
  }
}

export function weatherEmoji(icon: string): string {
  if (icon.startsWith('01')) return '☀️'
  if (icon.startsWith('02')) return '⛅'
  if (icon.startsWith('03') || icon.startsWith('04')) return '☁️'
  if (icon.startsWith('09') || icon.startsWith('10')) return '🌧'
  if (icon.startsWith('11')) return '⛈'
  if (icon.startsWith('13')) return '🌨'
  if (icon.startsWith('50')) return '🌫'
  return '🌤'
}

export function runningComment(temp: number, icon: string): string {
  if (icon.startsWith('09') || icon.startsWith('10') || icon.startsWith('11')) return '비가 옵니다. 미끄러움에 주의하세요 ☔'
  if (icon.startsWith('13')) return '눈이 옵니다. 안전에 주의하세요 ❄️'
  if (temp >= 30) return '매우 덥습니다. 수분 보충을 자주 해주세요 💧'
  if (temp >= 25) return '덥습니다. 수분 보충에 신경 쓰세요 🥤'
  if (temp >= 10 && temp < 20) return '러닝하기 최적의 날씨예요! 💪'
  if (temp >= 20) return '러닝하기 좋은 날씨예요 👍'
  if (temp < 0) return '매우 춥습니다. 동상에 주의하세요 🥶'
  if (temp < 5) return '춥습니다. 충분히 워밍업하세요 🧤'
  return '러닝하기 좋은 날씨예요 👍'
}
