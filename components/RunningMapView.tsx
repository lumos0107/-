import React, { useRef, useEffect, useCallback } from 'react'
import { StyleSheet } from 'react-native'
import WebView from 'react-native-webview'
import { RoutePoint } from '../utils/api'
import { getRunningMapHtml } from '../utils/mapHtml'

const MAPBOX_TOKEN = process.env.EXPO_PUBLIC_MAPBOX_TOKEN ?? ''

interface Props {
  points: RoutePoint[]
  currentLocation: RoutePoint | null
}

export default function RunningMapView({ points, currentLocation }: Props) {
  const webViewRef = useRef<WebView>(null)
  const currentLocationRef = useRef<RoutePoint | null>(currentLocation)

  useEffect(() => {
    currentLocationRef.current = currentLocation
  }, [currentLocation])

  const injectPosition = useCallback((loc: RoutePoint) => {
    webViewRef.current?.injectJavaScript(`
      updatePosition(${loc.latitude}, ${loc.longitude});
      true;
    `)
  }, [])

  // 위치 변경될 때마다 지도 업데이트
  useEffect(() => {
    if (!currentLocation) return
    injectPosition(currentLocation)
  }, [currentLocation])

  // WebView 로드 완료 시 현재 위치가 이미 있으면 즉시 이동
  function handleLoad() {
    const loc = currentLocationRef.current
    if (loc) injectPosition(loc)
  }

  return (
    <WebView
      ref={webViewRef}
      style={styles.map}
      source={{ html: getRunningMapHtml(MAPBOX_TOKEN, points, currentLocation ?? undefined) }}
      originWhitelist={['*']}
      scrollEnabled={false}
      javaScriptEnabled
      onLoad={handleLoad}
    />
  )
}

const styles = StyleSheet.create({
  map: { flex: 1, width: '100%' },
})
